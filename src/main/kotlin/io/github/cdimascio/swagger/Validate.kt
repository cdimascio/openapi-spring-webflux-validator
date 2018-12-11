package io.github.cdimascio.swagger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
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

    inner class Request(val request: ServerRequest, val objectMapperFactory: ObjectMapperFactory) {
        /**
         * Validates a request with body of type [bodyType] . If validation succeeds, the [handler]
         * is called to return a response
         */
        fun <T> withBody(bodyType: Class<T>, handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            return BodyValidator(request, bodyType, objectMapperFactory).validate(handler)
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
            val json = request.body(BodyExtractors.toMono(String::class.java))
            return json.flatMap { validator.validate(request, it) ?: handler(success(it)) }
        }
    }
}
