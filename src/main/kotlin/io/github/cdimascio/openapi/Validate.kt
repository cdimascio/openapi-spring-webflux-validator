package io.github.cdimascio.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import reactor.core.publisher.Mono

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

/**
 * Represents an error when validating a request against the
 * Swagger 2 or OpenApi 3 specification
 */
data class ValidationError(val code: Int, val message: String)

/**
 * Handler for validation errors
 */
typealias ErrorHandler<T> = (status: HttpStatus, List<String>) -> T

/**
 * Factory for ObjectMapper.
 */
typealias ObjectMapperFactory = () -> ObjectMapper

/**
 * Validates requests against a Swagger 2 or OpenAPI 3 specification.
 */
class Validate<out T> internal constructor(
    swaggerJsonPath: String,
    errorHandler: ErrorHandler<T>,
    private val objectMapperFactory: ObjectMapperFactory) {

    /**
     * The validate instance
     */
    companion object Instance {
        private val defaultErrorHandler: ErrorHandler<ValidationError> =
            { status, messages -> ValidationError(status.value(), messages[0]) }

        private val defaultObjectMapperFactory: ObjectMapperFactory = { jacksonObjectMapper() }

        /**
         * Configures the Validator with the Swagger 2 or OpenApi specification located at [openApiSwaggerPath]
         * The Swagger 2 or OpenApi 3 specification file may be represented as YAML or JSON.
         */
        fun configure(openApiSwaggerPath: String) = configure(openApiSwaggerPath, defaultErrorHandler)

        /**
         * Configures the Validator with the Swagger 2 or OpenApi specification located at [openApiSwaggerPath]
         * and a custom [errorHandler]. The specification file may be represented as YAML or JSON.
         */
        fun <T> configure(openApiSwaggerPath: String,
                          errorHandler: ErrorHandler<T>) = configure(openApiSwaggerPath, defaultObjectMapperFactory, errorHandler)


        /**
         * Configures the Validator with the Swagger 2 or OpenApi specification located at [openApiSwaggerPath], using
         * custom [objectMapperFactory] and a custom [errorHandler]. The specification file may be represented as YAML or JSON.
         */
        fun <T> configure(openApiSwaggerPath: String,
                          objectMapperFactory: ObjectMapperFactory,
                          errorHandler: ErrorHandler<T>) = Validate(openApiSwaggerPath, errorHandler, objectMapperFactory)
    }

    private val validator = Validator(swaggerJsonPath, errorHandler)

    /**
     * The [request] to validate
     */
    fun request(request: ServerRequest) = Request(request, objectMapperFactory)

    /**
     * Validates the [request]. If validation succeeds, the [handler] function is called to return a response
     */
    fun request(request: ServerRequest, handler: () -> Mono<ServerResponse>) = validator.validate(request) ?: handler()

    /**
     * Validates the [request]. If validation succeeds, the [handler] function is called to return a response.
     * It's a suspended alternative to a [request] method.
     */
    suspend fun requestAndAwait(request: ServerRequest, handler: suspend () -> ServerResponse): ServerResponse =
        validator.validateAndAwait(request) ?: handler()

    inner class Request(val request: ServerRequest, val objectMapperFactory: ObjectMapperFactory) {
        /**
         * Validates a request with body of type [bodyType]. If validation succeeds, the [handler]
         * is called to return a response
         */
        fun <T> withBody(bodyType: Class<T>, handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            return BodyValidator(request, bodyType, objectMapperFactory).validate(handler)
        }

        /**
         * Reified inline version of the [Request.withBody] function.
         * @param T type of the body.
         * @param handler handler function.
         * @return ServerResponse as a result of the call.
         */
        inline fun <reified T> withBody(noinline handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> =
            this.withBody(T::class.java, handler)

        /**
         * Validates a request with body of type [bodyType] . If validation succeeds, the [handler]
         * is called to return a response.
         * It's a suspended alternative to a [withBody] method.
         */
        suspend fun <T> awaitBody(bodyType: Class<T>, handler: suspend (T) -> ServerResponse): ServerResponse {
            return BodyValidator(request, bodyType, objectMapperFactory).validateAndAwait(handler)
        }
    }
    /**
     * Creates a new BodyValidator to validate a [request] of type [bodyType] using [objectMapperFactory].
     */
    inner class BodyValidator<T>(val request: ServerRequest, val bodyType: Class<T>, val objectMapperFactory: ObjectMapperFactory) {
        /**
         * Validates the body and calls [handler] if the validation succeeds
         */
        fun validate(handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            val success = { json: String -> objectMapperFactory().readValue(json, bodyType) }
            val json = request.body(BodyExtractors.toMono(String::class.java)).switchIfEmpty(Mono.just(""))
            return json.flatMap { validator.validate(request, it) ?: handler(success(it)) }
        }

        /**
         * Validates the body and calls [handler] if the validation succeeds.
         * It's a suspended alternative to a [validate] method.
         */
        suspend fun validateAndAwait(handler: suspend (T) -> ServerResponse): ServerResponse {
            val success = { json: String -> objectMapperFactory().readValue(json, bodyType) }
            val json = request.awaitBodyOrNull() ?: ""
            return json.let { validator.validateAndAwait(request, it) ?: handler(success(it)) }
        }
    }
}
