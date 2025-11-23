package com.project.lighthouse.data.api

sealed class ApiException(message: String) : Exception(message) {
    data class NetworkError(override val message: String) : ApiException(message)
    data class HttpError(val code: Int, override val message: String) : ApiException(message)
    data class UnknownError(override val message: String) : ApiException(message)
}

