package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private var currentBaseUrl: String = ""
    private var currentService: ApiService? = null

    @Synchronized
    fun getService(baseUrl: String): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (currentService == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .build()
            currentService = retrofit.create(ApiService::class.java)
        }
        return currentService!!
    }

    /**
     * Safely parse a JSON string into an object of type T.
     * Inspects if response is wrapped inside a "data" or "result" key or is root.
     */
    inline fun <reified T> parseObject(jsonStr: String): T? {
        val adapter = moshi.adapter(T::class.java)
        // 1. Try parsing root directly
        try {
            val root = adapter.fromJson(jsonStr)
            if (root != null) return root
        } catch (_: Exception) {}

        // 2. Check if wrapped in "data" or "result" or "user" or "account"
        try {
            val jsonObject = JSONObject(jsonStr)
            val candidateKeys = listOf("data", "result", "account", "user", "budget", "statistics")
            for (key in candidateKeys) {
                if (jsonObject.has(key) && !jsonObject.isNull(key)) {
                    val subJson = jsonObject.get(key).toString()
                    val parsed = adapter.fromJson(subJson)
                    if (parsed != null) return parsed
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Safely parse a JSON string into a List<T>.
     * Can parse direct JSON arrays or objects containing "data": [...], "transactions": [...], etc.
     */
    inline fun <reified T> parseList(jsonStr: String): List<T> {
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)

        val trimmed = jsonStr.trim()
        if (trimmed.startsWith("[")) {
            try {
                return adapter.fromJson(trimmed) ?: emptyList()
            } catch (_: Exception) {}
        }

        try {
            val jsonObject = JSONObject(trimmed)
            val candidateKeys = listOf("data", "result", "transactions", "txns", "budgets", "accounts", "items")
            for (key in candidateKeys) {
                if (jsonObject.has(key) && !jsonObject.isNull(key)) {
                    val item = jsonObject.get(key)
                    if (item is JSONArray) {
                        val parsed = adapter.fromJson(item.toString())
                        if (parsed != null) return parsed
                    }
                }
            }
        } catch (_: Exception) {}

        return emptyList()
    }

    /**
     * Extract an error or informative message from error or success JSON body.
     */
    fun extractMessage(jsonStr: String?): String? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(jsonStr)
            when {
                obj.has("message") && !obj.isNull("message") -> obj.getString("message")
                obj.has("error") && !obj.isNull("error") -> obj.getString("error")
                obj.has("detail") && !obj.isNull("detail") -> obj.getString("detail")
                obj.has("msg") && !obj.isNull("msg") -> obj.getString("msg")
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
