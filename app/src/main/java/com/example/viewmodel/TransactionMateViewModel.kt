package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.preferences.AppPreferences
import com.example.data.remote.NetworkResult
import com.example.data.repository.TransactionMateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Checking : ConnectionState()
    data class Connected(val url: String, val message: String) : ConnectionState()
    data class Error(val url: String, val message: String) : ConnectionState()
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val monthlyDebitTotal: Double = 0.0,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val statistics: TxnStatisticsResponse? = null,
    val errorMessage: String? = null
)

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val allTransactions: List<TransactionItem> = emptyList(),
    val filteredTransactions: List<TransactionItem> = emptyList(),
    val filterType: String = "ALL", // "ALL", "DEBIT", "CREDIT"
    val filterCategory: String = "ALL",
    val searchQuery: String = "",
    val errorMessage: String? = null
)

data class BudgetsUiState(
    val isLoading: Boolean = false,
    val budgets: List<BudgetItem> = emptyList(),
    val monthlySpent: Double = 0.0,
    val errorMessage: String? = null
)

data class QrUiState(
    val isLoading: Boolean = false,
    val qrList: List<QrItem> = emptyList(),
    val requestedAmount: Double? = null,
    val errorMessage: String? = null
)

class TransactionMateViewModel(
    private val repository: TransactionMateRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val baseUrl: StateFlow<String> = preferences.baseUrlFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_BASE_URL)

    val activeUsername: StateFlow<String> = preferences.activeUsernameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_USERNAME)

    val activeUserName: StateFlow<String> = preferences.activeUserNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "User")

    val currencySymbol: StateFlow<String> = preferences.currencyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_CURRENCY)

    val themeMode: StateFlow<com.example.ui.theme.AppThemeMode> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.ui.theme.AppThemeMode.SYSTEM)

    val themeColor: StateFlow<com.example.ui.theme.AppThemeColor> = preferences.themeColorFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.ui.theme.AppThemeColor.GREEN)

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: com.example.ui.theme.AppThemeColor) {
        viewModelScope.launch {
            preferences.setThemeColor(color)
        }
    }

    val userProfile: StateFlow<UserProfile?> = repository.currentUserProfile

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _transactionsState = MutableStateFlow(TransactionsUiState())
    val transactionsState: StateFlow<TransactionsUiState> = _transactionsState.asStateFlow()

    private val _budgetsState = MutableStateFlow(BudgetsUiState())
    val budgetsState: StateFlow<BudgetsUiState> = _budgetsState.asStateFlow()

    private val _qrState = MutableStateFlow(QrUiState())
    val qrState: StateFlow<QrUiState> = _qrState.asStateFlow()

    val biometricLockEnabled: StateFlow<Boolean> = preferences.biometricLockEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _biometricCapability = MutableStateFlow<com.example.data.security.BiometricCapability>(com.example.data.security.BiometricCapability.Available)
    val biometricCapability: StateFlow<com.example.data.security.BiometricCapability> = _biometricCapability.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _actionStatusMessage = MutableSharedFlow<String>()
    val actionStatusMessage: SharedFlow<String> = _actionStatusMessage.asSharedFlow()

    fun updateBiometricCapability(capability: com.example.data.security.BiometricCapability) {
        _biometricCapability.value = capability
    }

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
        if (!locked) {
            _authErrorMessage.value = null
        }
    }

    fun setAuthError(message: String?) {
        _authErrorMessage.value = message
    }

    fun setIsAuthenticating(authenticating: Boolean) {
        _isAuthenticating.value = authenticating
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBiometricLockEnabled(enabled)
            if (!enabled) {
                _isAppLocked.value = false
            }
        }
    }

    init {
        // Initial connection check and data load
        viewModelScope.launch {
            testConnection()
            refreshAll()
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Checking
            val url = baseUrl.value
            when (val result = repository.checkHealth()) {
                is NetworkResult.Success -> {
                    _connectionState.value = ConnectionState.Connected(
                        url = url,
                        message = result.data.message ?: "Service is available"
                    )
                }
                is NetworkResult.Error -> {
                    _connectionState.value = ConnectionState.Error(
                        url = url,
                        message = result.message
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun updateBaseUrl(newUrl: String) {
        viewModelScope.launch {
            preferences.setBaseUrl(newUrl)
            _actionStatusMessage.emit("Configuration updated")
            testConnection()
            refreshAll()
        }
    }

    fun switchActiveUser(username: String, name: String = "") {
        viewModelScope.launch {
            preferences.setActiveUser(username, name)
            _actionStatusMessage.emit("Active user switched to $username")
            refreshAll()
        }
    }

    fun refreshAll() {
        val user = activeUsername.value
        loadUserProfile(user)
        loadTransactions(user)
        loadBudgets(user)
        loadMonthlyTxn(user)
        loadStatistics(user)
        loadQrCodes(user = user)
    }

    fun loadUserProfile(user: String = activeUsername.value) {
        viewModelScope.launch {
            when (val result = repository.getAccount(user)) {
                is NetworkResult.Success -> {
                    calculateBalances(result.data.accounts, _transactionsState.value.allTransactions)
                }
                is NetworkResult.Error -> {
                    // Profile not yet created on backend or network error
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun createUser(
        name: String,
        mobileNumber: String,
        email: String,
        username: String,
        accounts: List<BankAccount>,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val req = CreateUserRequest(
                name = name.trim(),
                mobile = mobileNumber.trim(),
                email = email.trim(),
                username = username.trim(),
                accounts = accounts
            )
            when (val result = repository.createUser(req)) {
                is NetworkResult.Success -> {
                    _actionStatusMessage.emit("User profile created!")
                    refreshAll()
                    onComplete(true, "Profile created successfully!")
                }
                is NetworkResult.Error -> {
                    onComplete(false, result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadTransactions(user: String = activeUsername.value) {
        viewModelScope.launch {
            _transactionsState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getTransactions(user)) {
                is NetworkResult.Success -> {
                    // Sort by newest first
                    val sorted = result.data.reversed()
                    _transactionsState.update { current ->
                        current.copy(
                            isLoading = false,
                            allTransactions = sorted
                        )
                    }
                    applyFilters()
                    _dashboardState.update { it.copy(recentTransactions = sorted.take(5)) }
                    calculateBalances(userProfile.value?.accounts ?: emptyList(), sorted)
                }
                is NetworkResult.Error -> {
                    _transactionsState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun recordPayment(
        amount: Double,
        bankName: String,
        category: String,
        description: String,
        transactionDate: String,
        type: String,
        accountNumber: String?,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val user = activeUsername.value
            val req = CreatePaymentRequest(
                username = user,
                amount = amount,
                bankName = bankName,
                category = category,
                description = description,
                transactionDate = transactionDate,
                type = type.lowercase(),
                accountNumber = accountNumber
            )
            when (val result = repository.createPayment(req)) {
                is NetworkResult.Success -> {
                    _actionStatusMessage.emit(result.data)
                    // Refresh data
                    loadTransactions(user)
                    loadMonthlyTxn(user)
                    loadStatistics(user)
                    loadUserProfile(user)
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    onComplete(false, result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun filterTransactions(type: String? = null, category: String? = null, search: String? = null) {
        _transactionsState.update { current ->
            current.copy(
                filterType = type ?: current.filterType,
                filterCategory = category ?: current.filterCategory,
                searchQuery = search ?: current.searchQuery
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _transactionsState.value
        val filtered = state.allTransactions.filter { item ->
            val matchType = when (state.filterType.uppercase()) {
                "DEBIT" -> !item.isCredit
                "CREDIT" -> item.isCredit
                else -> true
            }
            val matchCat = if (state.filterCategory.uppercase() == "ALL") true else {
                item.category.equals(state.filterCategory, ignoreCase = true)
            }
            val matchSearch = if (state.searchQuery.isBlank()) true else {
                item.description.contains(state.searchQuery, ignoreCase = true) ||
                        item.bankName.contains(state.searchQuery, ignoreCase = true) ||
                        item.category.contains(state.searchQuery, ignoreCase = true)
            }
            matchType && matchCat && matchSearch
        }
        _transactionsState.update { it.copy(filteredTransactions = filtered) }
    }

    fun loadBudgets(user: String = activeUsername.value) {
        viewModelScope.launch {
            _budgetsState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getBudgets(user)) {
                is NetworkResult.Success -> {
                    _budgetsState.update {
                        it.copy(isLoading = false, budgets = result.data)
                    }
                }
                is NetworkResult.Error -> {
                    _budgetsState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun addBudget(amount: Double, category: String?, month: String?, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = activeUsername.value
            val req = AddBudgetRequest(
                username = user,
                amount = amount,
                category = category,
                month = month
            )
            when (val result = repository.addBudget(req)) {
                is NetworkResult.Success -> {
                    _actionStatusMessage.emit(result.data)
                    loadBudgets(user)
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    onComplete(false, result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun updateBudget(amount: Double, budgetId: String?, month: String?, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = activeUsername.value
            val req = UpdateBudgetRequest(
                username = user,
                amount = amount,
                budgetId = budgetId,
                month = month
            )
            when (val result = repository.updateBudget(req)) {
                is NetworkResult.Success -> {
                    _actionStatusMessage.emit(result.data)
                    loadBudgets(user)
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    onComplete(false, result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadMonthlyTxn(user: String = activeUsername.value) {
        viewModelScope.launch {
            when (val result = repository.getMonthlyTxn(user)) {
                is NetworkResult.Success -> {
                    _dashboardState.update { it.copy(monthlyDebitTotal = result.data) }
                    _budgetsState.update { it.copy(monthlySpent = result.data) }
                }
                is NetworkResult.Error -> Unit
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadStatistics(user: String = activeUsername.value) {
        viewModelScope.launch {
            when (val result = repository.getTxnStatistics(user)) {
                is NetworkResult.Success -> {
                    _dashboardState.update { it.copy(statistics = result.data) }
                }
                is NetworkResult.Error -> Unit
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadQrCodes(
        user: String = activeUsername.value,
        bankName: String? = null,
        upiVpa: String? = null,
        amount: Double? = null
    ) {
        viewModelScope.launch {
            _qrState.update { it.copy(isLoading = true, requestedAmount = amount, errorMessage = null) }
            val req = GetQrRequest(
                username = user,
                bankName = bankName,
                upiVpa = upiVpa,
                amount = amount
            )
            when (val result = repository.getQr(req)) {
                is NetworkResult.Success -> {
                    _qrState.update { it.copy(isLoading = false, qrList = result.data) }
                }
                is NetworkResult.Error -> {
                    _qrState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Explicit administrative action: GET /send/report/
     */
    fun triggerSendReport(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.sendReport()) {
                is NetworkResult.Success -> {
                    _actionStatusMessage.emit(result.data)
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    onComplete(false, result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    private fun calculateBalances(accounts: List<BankAccount>, txns: List<TransactionItem>) {
        val accountTotal = accounts.sumOf { it.balance }
        var income = 0.0
        var expenses = 0.0
        for (t in txns) {
            if (t.isCredit) income += t.amount else expenses += t.amount
        }
        val computedBalance = if (accountTotal > 0) accountTotal else (income - expenses).coerceAtLeast(0.0)

        _dashboardState.update {
            it.copy(
                totalBalance = computedBalance,
                totalIncome = income,
                totalExpenses = expenses
            )
        }
    }
}

class TransactionMateViewModelFactory(
    private val repository: TransactionMateRepository,
    private val preferences: AppPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionMateViewModel(repository, preferences) as T
    }
}
