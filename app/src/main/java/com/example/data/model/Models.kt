package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Common Bank Account model used in profile, payments, and QR generation
 */
@JsonClass(generateAdapter = true)
data class BankAccount(
    @Json(name = "type") val type: String = "Savings",
    @Json(name = "bank_name") val bankName: String = "",
    @Json(name = "account_number") val accountNumber: String = "",
    @Json(name = "balance") val balance: Double = 0.0,
    @Json(name = "upi_vpa") val upiVpa: String = ""
)

/**
 * User Profile / Account information
 */
@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "username") val username: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "mobile_number") val mobileNumber: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "accounts") val accounts: List<BankAccount> = emptyList()
)

/**
 * Request: POST /create
 */
@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    @Json(name = "name") val name: String,
    @Json(name = "mobile_number") val mobileNumber: String,
    @Json(name = "email") val email: String,
    @Json(name = "username") val username: String,
    @Json(name = "accounts") val accounts: List<BankAccount>? = null
)

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
