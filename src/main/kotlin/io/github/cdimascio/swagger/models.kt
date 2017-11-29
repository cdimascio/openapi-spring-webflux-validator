package io.github.cdimascio.swagger

data class ErrorDetail(val code: String, val message: String)
data class ServiceError(val trace: String, val errors: List<ErrorDetail>)