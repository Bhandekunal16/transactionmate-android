package com.example.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Common Bank Account model used in profile, payments, and QR generation.
 * Custom JSON adapter serializes to { "type", "bankName", "number", "balance", "vpa" }
 * and flexibly deserializes both camelCase and snake_case variants.
 */
data class BankAccount(
    val type: String = "Savings",
    val bankName: String = "",
    val accountNumber: String = "",
    val balance: Double = 0.0,
    val upiVpa: String = ""
) {
    fun toAccountRequest(): AccountRequest = AccountRequest(
        type = type,
        bankName = bankName,
        number = accountNumber,
        balance = balance,
        vpa = upiVpa
    )
}

/**
 * Dedicated Account request data model for POST /create matching api.doc schema.
 */
@JsonClass(generateAdapter = true)
data class AccountRequest(
    @Json(name = "type") val type: String = "savings",
    @Json(name = "bankName") val bankName: String = "",
    @Json(name = "number") val number: String = "",
    @Json(name = "balance") val balance: Double = 0.0,
    @Json(name = "vpa") val vpa: String = ""
) {
    fun toBankAccount(): BankAccount = BankAccount(
        type = type,
        bankName = bankName,
        accountNumber = number,
        balance = balance,
        upiVpa = vpa
    )
}

class BankAccountJsonAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): BankAccount {
        var type = "Savings"
        var bankName = ""
        var accountNumber = ""
        var balance = 0.0
        var upiVpa = ""

        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any?>()
            return BankAccount()
        }

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> {
                    type = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        "Savings"
                    } else reader.nextString()
                }
                "bankName", "bank_name" -> {
                    bankName = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                "number", "account_number", "accountNumber" -> {
                    accountNumber = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                "balance" -> {
                    balance = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        0.0
                    } else {
                        try {
                            reader.nextDouble()
                        } catch (_: Exception) {
                            reader.nextString().toDoubleOrNull() ?: 0.0
                        }
                    }
                }
                "vpa", "upi_vpa", "upiVpa" -> {
                    upiVpa = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return BankAccount(type, bankName, accountNumber, balance, upiVpa)
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: BankAccount?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("type").value(value.type)
        writer.name("bankName").value(value.bankName)
        writer.name("number").value(value.accountNumber)
        writer.name("balance").value(value.balance)
        writer.name("vpa").value(value.upiVpa)
        writer.endObject()
    }
}

/**
 * User Profile / Account information.
 * Custom JSON adapter handles both "mobile" and "mobile_number".
 */
data class UserProfile(
    val username: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val accounts: List<BankAccount> = emptyList()
) {
    val displayMobile: String get() = mobileNumber
}

class UserProfileJsonAdapter {
    @FromJson
    fun fromJson(reader: JsonReader, accountsAdapter: JsonAdapter<List<BankAccount>>): UserProfile {
        var username = ""
        var name = ""
        var mobileNumber = ""
        var email = ""
        var accounts: List<BankAccount> = emptyList()

        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any?>()
            return UserProfile()
        }

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "username" -> {
                    username = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                "name" -> {
                    name = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                "mobile", "mobile_number", "mobileNumber" -> {
                    mobileNumber = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                "email" -> {
                    email = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        ""
                    } else reader.nextString()
                }
                "accounts" -> {
                    accounts = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any?>()
                        emptyList()
                    } else {
                        accountsAdapter.fromJson(reader) ?: emptyList()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return UserProfile(username, name, mobileNumber, email, accounts)
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: UserProfile?, accountsAdapter: JsonAdapter<List<BankAccount>>) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("username").value(value.username)
        writer.name("name").value(value.name)
        writer.name("mobile").value(value.mobileNumber)
        writer.name("email").value(value.email)
        writer.name("accounts")
        accountsAdapter.toJson(writer, value.accounts)
        writer.endObject()
    }
}

/**
 * Request: POST /create
 * Matching the documented schema:
 * {
 *   "name": "Asha Patel",
 *   "mobile": "9876543210",
 *   "email": "asha@example.com",
 *   "username": "asha",
 *   "accounts": [
 *     {
 *       "type": "savings",
 *       "bankName": "Example Bank",
 *       "number": "1234567890",
 *       "balance": 2500.0,
 *       "vpa": "asha@example"
 *     }
 *   ]
 * }
 */
@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    @Json(name = "name") val name: String,
    @Json(name = "mobile") val mobile: String,
    @Json(name = "email") val email: String,
    @Json(name = "username") val username: String,
    @Json(name = "accounts") val accounts: List<BankAccount>? = null
) {
    val mobileNumber: String
        get() = mobile
}

/**
 * Request: POST /create/payment
 */
@JsonClass(generateAdapter = true)
data class CreatePaymentRequest(
    @Json(name = "username") val username: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "bank_name") val bankName: String,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String,
    @Json(name = "transaction_date") val transactionDate: String,
    @Json(name = "type") val type: String, // "debit" or "credit"
    @Json(name = "account_number") val accountNumber: String? = null
)

/**
 * Transaction Item model
 */
@JsonClass(generateAdapter = true)
data class TransactionItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "txn_id") val txnId: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "bank_name") val bankName: String = "",
    @Json(name = "category") val category: String = "General",
    @Json(name = "description") val description: String = "",
    @Json(name = "transaction_date") val transactionDate: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "type") val type: String = "debit", // "debit" or "credit"
    @Json(name = "account_number") val accountNumber: String? = null
) {
    val displayDate: String
        get() = transactionDate ?: date ?: "Recent"

    val isCredit: Boolean
        get() = type.equals("credit", ignoreCase = true)
}

/**
 * Request: POST /add/budget
 */
@JsonClass(generateAdapter = true)
data class AddBudgetRequest(
    @Json(name = "username") val username: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String? = null,
    @Json(name = "month") val month: String? = null
)

/**
 * Request: POST /update/budget
 */
@JsonClass(generateAdapter = true)
data class UpdateBudgetRequest(
    @Json(name = "username") val username: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "budget_id") val budgetId: String? = null,
    @Json(name = "month") val month: String? = null
)

/**
 * Budget Item model
 */
@JsonClass(generateAdapter = true)
data class BudgetItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "budget_id") val budgetId: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "spent") val spent: Double? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "month") val month: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

/**
 * Generic Request with username
 */
@JsonClass(generateAdapter = true)
data class UsernameOnlyRequest(
    @Json(name = "username") val username: String
)

/**
 * Request: POST /get/monthly/txn
 */
@JsonClass(generateAdapter = true)
data class MonthlyTxnRequest(
    @Json(name = "username") val username: String,
    @Json(name = "month") val month: String? = null,
    @Json(name = "year") val year: String? = null
)

/**
 * Response: POST /get/monthly/txn
 */
@JsonClass(generateAdapter = true)
data class MonthlyTxnResponse(
    @Json(name = "total_debit") val totalDebit: Double? = null,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "month") val month: String? = null,
    @Json(name = "message") val message: String? = null
) {
    val totalDebitAmount: Double
        get() = totalDebit ?: amount ?: 0.0
}

/**
 * Date-wise statistics point
 */
@JsonClass(generateAdapter = true)
data class DateWiseStat(
    @Json(name = "date") val date: String = "",
    @Json(name = "debit") val debit: Double = 0.0,
    @Json(name = "credit") val credit: Double = 0.0,
    @Json(name = "count") val count: Int = 0
)

/**
 * Response: POST /get/txn/statistics
 */
@JsonClass(generateAdapter = true)
data class TxnStatisticsResponse(
    @Json(name = "total_debit_count") val totalDebitCount: Int? = null,
    @Json(name = "total_debit_amount") val totalDebitAmount: Double? = null,
    @Json(name = "total_credit_count") val totalCreditCount: Int? = null,
    @Json(name = "total_credit_amount") val totalCreditAmount: Double? = null,
    @Json(name = "date_wise") val dateWise: List<DateWiseStat>? = null,
    @Json(name = "date_wise_stats") val dateWiseStats: List<DateWiseStat>? = null,
    @Json(name = "message") val message: String? = null
) {
    val debitCount: Int get() = totalDebitCount ?: 0
    val debitTotal: Double get() = totalDebitAmount ?: 0.0
    val creditCount: Int get() = totalCreditCount ?: 0
    val creditTotal: Double get() = totalCreditAmount ?: 0.0
    val dailyList: List<DateWiseStat> get() = dateWise ?: dateWiseStats ?: emptyList()
}

/**
 * Request: POST /get/qr
 */
@JsonClass(generateAdapter = true)
data class GetQrRequest(
    @Json(name = "username") val username: String,
    @Json(name = "bank_name") val bankName: String? = null,
    @Json(name = "account_number") val accountNumber: String? = null,
    @Json(name = "upi_vpa") val upiVpa: String? = null,
    @Json(name = "amount") val amount: Double? = null
)

/**
 * QR Code item representation
 */
@JsonClass(generateAdapter = true)
data class QrItem(
    @Json(name = "bank_name") val bankName: String? = null,
    @Json(name = "account_number") val accountNumber: String? = null,
    @Json(name = "upi_vpa") val upiVpa: String? = null,
    @Json(name = "qr_code") val qrCode: String? = null,
    @Json(name = "qr_image") val qrImage: String? = null,
    @Json(name = "upi_url") val upiUrl: String? = null,
    @Json(name = "amount") val amount: Double? = null
) {
    val effectiveQrContent: String
        get() = qrCode ?: qrImage ?: upiUrl ?: upiVpa.orEmpty()
}

/**
 * Health Check response
 */
@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null
)
