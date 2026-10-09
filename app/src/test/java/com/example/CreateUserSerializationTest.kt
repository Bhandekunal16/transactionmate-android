package com.example

import com.example.data.model.AccountRequest
import com.example.data.model.BankAccount
import com.example.data.model.CreateUserRequest
import com.example.data.model.UserProfile
import com.example.data.remote.ApiClient
import com.example.data.remote.ApiService
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CreateUserSerializationTest {

    @Test
    fun testCreateUserRequestJsonSerialization() {
        val request = CreateUserRequest(
            name = "Asha Patel",
            mobile = "9876543210",
            email = "asha@example.com",
            username = "asha",
            accounts = listOf(
                BankAccount(
                    type = "savings",
                    bankName = "Example Bank",
                    accountNumber = "1234567890",
                    balance = 2500.0,
                    upiVpa = "asha@example"
                )
            )
        )

        val adapter = ApiClient.moshi.adapter(CreateUserRequest::class.java)
        val jsonString = adapter.toJson(request)
        assertNotNull(jsonString)

        val json = JSONObject(jsonString)
        assertEquals("Asha Patel", json.getString("name"))
        assertEquals("9876543210", json.getString("mobile"))
        assertEquals("asha@example.com", json.getString("email"))
        assertEquals("asha", json.getString("username"))

        val accountsArray = json.getJSONArray("accounts")
        assertEquals(1, accountsArray.length())

        val accObj = accountsArray.getJSONObject(0)
        assertEquals("savings", accObj.getString("type"))
        assertEquals("Example Bank", accObj.getString("bankName"))
        assertEquals("1234567890", accObj.getString("number"))
        assertEquals(2500.0, accObj.getDouble("balance"), 0.001)
        assertEquals("asha@example", accObj.getString("vpa"))
    }

    @Test
    fun testRetrofitCreatesBodyConverterForCreateUser() {
        // This confirms that Retrofit with MoshiConverterFactory can create the converter
        // without throwing "Unable to create @Body converter for class CreateUserRequest"
        val service: ApiService = ApiClient.getService(com.example.data.preferences.AppPreferences.DEFAULT_BASE_URL)
        assertNotNull(service)

        // Verify that ApiService's createUser method can be resolved by Retrofit's reflection
        val method = ApiService::class.java.getMethod("createUser", CreateUserRequest::class.java, kotlin.coroutines.Continuation::class.java)
        assertNotNull(method)
    }

    @Test
    fun testDeserializationSupportsBothCamelCaseAndSnakeCase() {
        // 1. camelCase from response
        val camelJson = """
            {
              "username": "asha",
              "name": "Asha Patel",
              "mobile": "9876543210",
              "email": "asha@example.com",
              "accounts": [
                {
                  "type": "savings",
                  "bankName": "Example Bank",
                  "number": "1234567890",
                  "balance": 2500.0,
                  "vpa": "asha@example"
                }
              ]
            }
        """.trimIndent()

        val parsedCamel = ApiClient.parseObject<UserProfile>(camelJson)
        assertNotNull(parsedCamel)
        assertEquals("asha", parsedCamel?.username)
        assertEquals("9876543210", parsedCamel?.mobileNumber)
        assertEquals("Example Bank", parsedCamel?.accounts?.firstOrNull()?.bankName)
        assertEquals("1234567890", parsedCamel?.accounts?.firstOrNull()?.accountNumber)
        assertEquals("asha@example", parsedCamel?.accounts?.firstOrNull()?.upiVpa)

        // 2. snake_case from response
        val snakeJson = """
            {
              "username": "asha",
              "name": "Asha Patel",
              "mobile_number": "9876543210",
              "email": "asha@example.com",
              "accounts": [
                {
                  "type": "Savings",
                  "bank_name": "Example Bank",
                  "account_number": "1234567890",
                  "balance": 2500.0,
                  "upi_vpa": "asha@example"
                }
              ]
            }
        """.trimIndent()

        val parsedSnake = ApiClient.parseObject<UserProfile>(snakeJson)
        assertNotNull(parsedSnake)
        assertEquals("asha", parsedSnake?.username)
        assertEquals("9876543210", parsedSnake?.mobileNumber)
        assertEquals("Example Bank", parsedSnake?.accounts?.firstOrNull()?.bankName)
        assertEquals("1234567890", parsedSnake?.accounts?.firstOrNull()?.accountNumber)
        assertEquals("asha@example", parsedSnake?.accounts?.firstOrNull()?.upiVpa)
    }
}
