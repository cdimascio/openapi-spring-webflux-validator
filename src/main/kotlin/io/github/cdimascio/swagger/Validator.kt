package io.github.cdimascio.swagger

import com.atlassian.oai.validator.SwaggerRequestResponseValidator
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.report.ValidationReport
import org.springframework.web.reactive.function.server.ServerRequest


internal object Validator {
    private val swaggerValidator = SwaggerRequestResponseValidator
            .createFor("static/api.json")
            .build();

    fun validate(request: ServerRequest, body: String? = null): ServiceError? {
        val builder = createSimpleRequestBuilder(request)
        body?.let { builder.withBody(body) }
        val simpleRequest = builder.build()

        val report = swaggerValidator.validateRequest(simpleRequest);
        println("has errors ${report.hasErrors()}")
        return if (report.hasErrors()) errorFrom(report = report) else null
    }

    private fun errorFrom(report: ValidationReport): ServiceError {
        println("called errorFrom, respond bad request")
        val message = report.messages[0]
        val detail = ErrorDetail("bad_request", message = message.message)
        return ServiceError(trace = "aaaabbbbb", errors = listOf(detail))
    }

    private fun createSimpleRequestBuilder(request: ServerRequest): SimpleRequest.Builder{
        val method = com.atlassian.oai.validator.model.Request.Method.valueOf(request.methodName())
        val requestBuilder = SimpleRequest.Builder(method, request.path())
        val headerNames = request.headers().asHttpHeaders().keys.asIterable()

        request.queryParams().entries.forEach { requestBuilder.withQueryParam(it.key, it.value) }
        headerNames.forEach { requestBuilder.withHeader(it, request.headers().header(it)) }

        return requestBuilder
    }
}