package io.github.cdimascio.openapi

import org.springframework.web.reactive.function.server.ServerRequest

data class MyError(val request: ServerRequest, val code: Int, val name: String)
