package io.github.cdimascio.openapi

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.SimpleRequest
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Mono

internal class Validator<out T>(swaggerJsonPath: String, private val errorHandler: (status: HttpStatus, List<String>) -> T) {
    private operator fun Regex.contains(text: CharSequence) = this.matches(text)
    private val swaggerValidator = OpenApiInteractionValidator
        .createFor(swaggerJsonPath)
        .build()

    fun validate(request: ServerRequest, body: String? = null): Mono<ServerResponse>? {
        val builder = createSimpleRequestBuilder(request)
        body?.let { builder.withBody(body) }
        val simpleRequest = builder.build()

        val report = swaggerValidator.validateRequest(simpleRequest)
        return if (report.hasErrors()) {
            val status = status(report.messages[0].key)
            val messages = report.messages.map { it.message }
            val error = errorHandler(status, messages)
            val e = BodyInserters.fromObject(error)
            status(status).body(e)
        } else null
    }

    suspend fun validateAndAwait(request: ServerRequest, body: String? = null): ServerResponse? {
        val builder = createSimpleRequestBuilder(request)
        body?.let { builder.withBody(body) }
        val simpleRequest = builder.build()

        val report = swaggerValidator.validateRequest(simpleRequest)
        return if (report.hasErrors()) {
            val status = status(report.messages[0].key)
            val messages = report.messages.map { it.message }
            val error = errorHandler(status, messages)
            status(status).bodyValueAndAwait(error as Any)
        } else null
    }

    private fun status(key: String) = when (key) {
        in Regex("validation.request.contentType.notAllowed") -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
        in Regex("validation.request.contentType.invalid") -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
        in Regex("validation.request.path.missing") -> HttpStatus.NOT_FOUND
        in Regex("validation.request.accept.invalid") -> HttpStatus.NOT_ACCEPTABLE
        in Regex("validation.request.operation.notAllowed") -> HttpStatus.METHOD_NOT_ALLOWED
        // TODO map any other 40X cases above
        else -> HttpStatus.BAD_REQUEST
    }

    private fun createSimpleRequestBuilder(request: ServerRequest): SimpleRequest.Builder {
        val method = com.atlassian.oai.validator.model.Request.Method.valueOf(request.methodName())
        val requestBuilder = SimpleRequest.Builder(method, request.path())
        val headerNames = request.headers().asHttpHeaders().keys.asIterable()

        request.queryParams().entries.forEach { requestBuilder.withQueryParam(it.key, it.value) }
        headerNames.forEach { requestBuilder.withHeader(it, request.headers().header(it)) }

        return requestBuilder
    }
}
