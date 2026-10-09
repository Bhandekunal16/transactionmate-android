package com.example.data.repository

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
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response

class TransactionMateRepository(
    private val preferences: AppPreferences
) {
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private suspend fun getService(): ApiService {
        val baseUrl = preferences.baseUrlFlow.first()
        return ApiClient.getService(baseUrl)
    }

    /**
     * Test connection to backend health check GET /
     */
    suspend fun checkHealth(): NetworkResult<HealthCheckResponse> = withContext(Dispatchers.IO) {
        val baseUrl = preferences.baseUrlFlow.first()
        try {
            val response = getService().healthCheck()
            val raw = response.body()?.string() ?: ""
            if (response.isSuccessful) {
                val parsed = ApiClient.parseObject<HealthCheckResponse>(raw)
                    ?: HealthCheckResponse(status = "ok", message = raw.ifBlank { "Service is reachable" })
                NetworkResult.Success(parsed, "Connected to $baseUrl")
            } else {
                val errorMsg = ApiClient.extractMessage(raw) ?: "Server responded with HTTP ${response.code()}"
                NetworkResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = "Failed to connect to $baseUrl: ${e.localizedMessage ?: "Connection refused"}",
                cause = e
            )
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
                NetworkResult.Success(parsed, ApiClient.extractMessage(raw) ?: "User created successfully")
            } else {
                val msg = ApiClient.extractMessage(raw)
                    ?: "Failed to create user (HTTP ${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                    // Try parsing accounts list directly
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
                val msg = ApiClient.extractMessage(raw) ?: "User account not found (HTTP ${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                val msg = ApiClient.extractMessage(raw) ?: "Transaction recorded successfully"
                NetworkResult.Success(msg)
            } else {
                val msg = ApiClient.extractMessage(raw) ?: "Failed to record transaction (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                val msg = ApiClient.extractMessage(raw) ?: "Failed to fetch transactions (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                val msg = ApiClient.extractMessage(raw) ?: "Budget created successfully"
                NetworkResult.Success(msg)
            } else {
                val msg = ApiClient.extractMessage(raw) ?: "Failed to create budget (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                // Could be list of budgets or single budget object
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
                val msg = ApiClient.extractMessage(raw) ?: "Failed to retrieve budgets (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                val msg = ApiClient.extractMessage(raw) ?: "Budget updated successfully"
                NetworkResult.Success(msg)
            } else {
                val msg = ApiClient.extractMessage(raw) ?: "Failed to update budget (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                val msg = ApiClient.extractMessage(raw) ?: "Failed to retrieve statistics (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                    // Try parsing as simple number or object with amount
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
                val msg = ApiClient.extractMessage(raw) ?: "Failed to fetch monthly total (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                val msg = ApiClient.extractMessage(raw) ?: "Financial report dispatched via email successfully"
                NetworkResult.Success(msg)
            } else {
                val msg = ApiClient.extractMessage(raw) ?: "Failed to send report (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
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
                // Try parsing list first
                val list = ApiClient.parseList<QrItem>(raw)
                if (list.isNotEmpty()) {
                    NetworkResult.Success(list)
                } else {
                    // Try parsing single QrItem
                    val single = ApiClient.parseObject<QrItem>(raw)
                    if (single != null) {
                        NetworkResult.Success(listOf(single))
                    } else {
                        // If backend returns raw string (like base64 or UPI string)
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
                val msg = ApiClient.extractMessage(raw) ?: "Failed to generate QR (${response.code()})"
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}", cause = e)
        }
    }
}
