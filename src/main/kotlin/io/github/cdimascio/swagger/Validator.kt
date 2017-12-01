package io.github.cdimascio.swagger

import com.atlassian.oai.validator.SwaggerRequestResponseValidator
import com.atlassian.oai.validator.model.SimpleRequest
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

internal class Validator<out T>(private val swaggerJsonPath: String, private val errorHandler: (List<String>) -> T) {
    private val swaggerValidator = SwaggerRequestResponseValidator
            .createFor(swaggerJsonPath)
            .build()

    fun validate(request: ServerRequest, body: String? = null): Mono<ServerResponse>? {
        val builder = createSimpleRequestBuilder(request)
        body?.let { builder.withBody(body) }
        val simpleRequest = builder.build()

        val report = swaggerValidator.validateRequest(simpleRequest);

        return if (report.hasErrors()) {
            val error = errorHandler(report.messages.map { it.message })
            val v = ok().body(BodyInserters.fromObject(error))
            v
        } else null
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