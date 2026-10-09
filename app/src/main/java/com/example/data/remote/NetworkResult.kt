package com.example.data.remote

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T, val message: String? = null) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null, val cause: Throwable? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
}
