package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.example.data.preferences.AppPreferences
import com.example.data.remote.ApiClient
import com.example.data.remote.ApiService
import com.example.data.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TransactionMateRepository(
    private val preferences: AppPreferences
) {
    companion object {
        private const val TAG = "TransactionMateRepo"
    }

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private suspend fun getService(): ApiService {
        val baseUrl = preferences.baseUrlFlow.first()
        return ApiClient.getService(baseUrl)
    }

    /**
     * Map low-level network exceptions to user-friendly messages.
     * Raw exceptions, hostnames, IP addresses, and ports are never shown to the user.
     */
    private fun handleException(e: Exception, fallbackMessage: String): NetworkResult.Error {
        Log.e(TAG, "Network operation encountered exception: ${e.javaClass.simpleName}", e)
        val userFriendlyMessage = when (e) {
            is SocketTimeoutException -> "The request took too long. Please try again."
            is UnknownHostException -> "Unable to connect. Please check your internet connection and try again."
            is ConnectException -> "The service is temporarily unavailable. Please try again later."
            is IOException -> "Unable to connect. Please check your internet connection and try again."
            else -> fallbackMessage
        }
        return NetworkResult.Error(userFriendlyMessage, cause = e)
    }

    /**
     * Sanitize server response messages so no raw technical/HTTP codes or server details leak.
     */
    private fun sanitizeMessage(rawMsg: String?, fallbackMessage: String): String {
        val extracted = ApiClient.extractMessage(rawMsg)
        return if (!extracted.isNullOrBlank()) extracted else fallbackMessage
    }

    /**
     * Check backend reachability
     */
    suspend fun checkHealth(): NetworkResult<HealthCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val response = getService().healthCheck()
            val raw = response.body()?.string() ?: ""
            if (response.isSuccessful) {
                val parsed = ApiClient.parseObject<HealthCheckResponse>(raw)
                    ?: HealthCheckResponse(status = "ok", message = "Service is online")
                NetworkResult.Success(parsed, "Service is online")
            } else {
                NetworkResult.Error("The service is temporarily unavailable. Please try again later.", response.code())
            }
        } catch (e: Exception) {
            handleException(e, "The service is temporarily unavailable. Please try again later.")
        }
    }

    /**
     * POST /create - Create a new user profile
     */
    suspend fun createUser(request: CreateUserRequest): NetworkResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = getService().createUser(request)
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val parsed = ApiClient.parseObject<UserProfile>(raw)
                    ?: UserProfile(
                        username = request.username,
                        name = request.name,
                        mobileNumber = request.mobileNumber,
                        email = request.email,
                        accounts = request.accounts ?: emptyList()
                    )
                _currentUserProfile.value = parsed
                preferences.setActiveUser(parsed.username, parsed.name)
                NetworkResult.Success(parsed, "User profile created successfully")
            } else {
                val msg = sanitizeMessage(raw, "Unable to create your profile. Please check your details and try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to create your profile. Please check your details and try again.")
        }
    }

    /**
     * POST /get/account - Retrieve user account and bank information
     */
    suspend fun getAccount(username: String): NetworkResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = getService().getAccount(UsernameOnlyRequest(username))
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val profile = ApiClient.parseObject<UserProfile>(raw)
                if (profile != null) {
                    _currentUserProfile.value = profile
                    preferences.setActiveUser(profile.username, profile.name)
                    NetworkResult.Success(profile)
                } else {
                    val accountsList = ApiClient.parseList<BankAccount>(raw)
                    val synthesized = UserProfile(
                        username = username,
                        name = preferences.activeUserNameFlow.first(),
                        accounts = accountsList
                    )
                    _currentUserProfile.value = synthesized
                    NetworkResult.Success(synthesized)
                }
            } else {
                val msg = sanitizeMessage(raw, "Unable to load profile. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to load profile. Please check your connection.")
        }
    }

    /**
     * POST /create/payment - Record a debit or credit transaction
     */
    suspend fun createPayment(request: CreatePaymentRequest): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = getService().createPayment(request)
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val msg = sanitizeMessage(raw, "Transaction recorded successfully")
                NetworkResult.Success(msg)
            } else {
                val msg = sanitizeMessage(raw, "Unable to record transaction. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to record transaction. Please try again.")
        }
    }

    /**
     * POST /get/txn - Retrieve transaction history
     */
    suspend fun getTransactions(username: String): NetworkResult<List<TransactionItem>> = withContext(Dispatchers.IO) {
        try {
            val response = getService().getTransactions(UsernameOnlyRequest(username))
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val list = ApiClient.parseList<TransactionItem>(raw)
                NetworkResult.Success(list)
            } else {
                val msg = sanitizeMessage(raw, "Unable to load transaction history. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to load transaction history. Please try again.")
        }
    }

    /**
     * POST /add/budget - Create budget
     */
    suspend fun addBudget(request: AddBudgetRequest): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = getService().addBudget(request)
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val msg = sanitizeMessage(raw, "Budget created successfully")
                NetworkResult.Success(msg)
            } else {
                val msg = sanitizeMessage(raw, "Unable to create budget. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to create budget. Please try again.")
        }
    }

    /**
     * POST /get/budget - Retrieve budgets
     */
    suspend fun getBudgets(username: String): NetworkResult<List<BudgetItem>> = withContext(Dispatchers.IO) {
        try {
            val response = getService().getBudget(UsernameOnlyRequest(username))
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val list = ApiClient.parseList<BudgetItem>(raw)
                if (list.isNotEmpty()) {
                    NetworkResult.Success(list)
                } else {
                    val single = ApiClient.parseObject<BudgetItem>(raw)
                    if (single != null && single.amount > 0) {
                        NetworkResult.Success(listOf(single))
                    } else {
                        NetworkResult.Success(emptyList())
                    }
                }
            } else {
                val msg = sanitizeMessage(raw, "Unable to load budgets. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to load budgets. Please try again.")
        }
    }

    /**
     * POST /update/budget - Update budget amount
     */
    suspend fun updateBudget(request: UpdateBudgetRequest): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = getService().updateBudget(request)
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val msg = sanitizeMessage(raw, "Budget updated successfully")
                NetworkResult.Success(msg)
            } else {
                val msg = sanitizeMessage(raw, "Unable to update budget. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to update budget. Please try again.")
        }
    }

    /**
     * POST /get/txn/statistics - Retrieve transaction statistics
     */
    suspend fun getTxnStatistics(username: String): NetworkResult<TxnStatisticsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = getService().getTxnStatistics(UsernameOnlyRequest(username))
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val stats = ApiClient.parseObject<TxnStatisticsResponse>(raw)
                    ?: TxnStatisticsResponse()
                NetworkResult.Success(stats)
            } else {
                val msg = sanitizeMessage(raw, "Unable to load financial statistics. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to load financial statistics. Please try again.")
        }
    }

    /**
     * POST /get/monthly/txn - Retrieve current month debit total
     */
    suspend fun getMonthlyTxn(username: String, month: String? = null, year: String? = null): NetworkResult<Double> = withContext(Dispatchers.IO) {
        try {
            val response = getService().getMonthlyTxn(MonthlyTxnRequest(username = username, month = month, year = year))
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val parsed = ApiClient.parseObject<MonthlyTxnResponse>(raw)
                val total = parsed?.totalDebitAmount ?: run {
                    try {
                        val json = JSONObject(raw)
                        when {
                            json.has("total_debit") -> json.getDouble("total_debit")
                            json.has("amount") -> json.getDouble("amount")
                            json.has("total") -> json.getDouble("total")
                            json.has("data") -> json.getDouble("data")
                            else -> 0.0
                        }
                    } catch (_: Exception) {
                        0.0
                    }
                }
                NetworkResult.Success(total)
            } else {
                val msg = sanitizeMessage(raw, "Unable to load monthly spending summary. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to load monthly spending summary. Please try again.")
        }
    }

    /**
     * GET /send/report/ - Explicitly trigger financial report email
     */
    suspend fun sendReport(): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = getService().sendReport()
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()
            if (response.isSuccessful) {
                val msg = sanitizeMessage(raw, "Financial report dispatched successfully")
                NetworkResult.Success(msg)
            } else {
                val msg = sanitizeMessage(raw, "Unable to send report. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to send report. Please try again.")
        }
    }

    /**
     * POST /get/qr - Request payment QR codes for accounts
     */
    suspend fun getQr(request: GetQrRequest): NetworkResult<List<QrItem>> = withContext(Dispatchers.IO) {
        try {
            val response = getService().getQr(request)
            val raw = response.body()?.string() ?: response.errorBody()?.string().orEmpty()

            if (response.isSuccessful) {
                val list = ApiClient.parseList<QrItem>(raw)
                if (list.isNotEmpty()) {
                    NetworkResult.Success(list)
                } else {
                    val single = ApiClient.parseObject<QrItem>(raw)
                    if (single != null) {
                        NetworkResult.Success(listOf(single))
                    } else {
                        if (raw.isNotBlank()) {
                            NetworkResult.Success(
                                listOf(
                                    QrItem(
                                        bankName = request.bankName ?: "Primary Account",
                                        upiVpa = request.upiVpa,
                                        qrCode = raw.trim().removeSurrounding("\""),
                                        amount = request.amount
                                    )
                                )
                            )
                        } else {
                            NetworkResult.Success(emptyList())
                        }
                    }
                }
            } else {
                val msg = sanitizeMessage(raw, "Unable to generate payment QR code. Please try again.")
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            handleException(e, "Unable to generate payment QR code. Please try again.")
        }
    }
}
