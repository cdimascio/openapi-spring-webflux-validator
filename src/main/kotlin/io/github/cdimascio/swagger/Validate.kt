package io.github.cdimascio.swagger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)
data class ValidationError(val code: Int, val message: String)

typealias ErrorHandler<T> = (status: HttpStatus, List<String>) -> T
class Validate<out T> internal constructor(
        swaggerJsonPath: String,
        errorHandler: ErrorHandler<T>) {

    /**
     * Returns an instance of Validate
     */
    companion object Instance {
        private val defaultErrorHandler: ErrorHandler<ValidationError> =
                { status, messages -> ValidationError(status.value(), messages[0]) }

        /**
         * Configure the validator by specifying the path to a Swagger v2 specification
         * @param swaggerJsonPath path to a json formatted v2 swagger specification
         * @return the validate object
         */
        fun configure(swaggerJsonPath: String) = configure(swaggerJsonPath, defaultErrorHandler)

        /**
         * Configure the validator by specifying the path to a Swagger v2 JSON specification and a custom error handler
         * @param swaggerJsonPath path to a json formatted v2 swagger specification
         * @param errorHandler custom validation error handler
         * @return the validate object
         */
        fun <T> configure(swaggerJsonPath: String, errorHandler: ErrorHandler<T>) = Validate(swaggerJsonPath, errorHandler)
    }

    private val validator = Validator(swaggerJsonPath, errorHandler)

    /**
     * @param request the server request, typically used withBody
     * @return Request
     */
    fun request(request: ServerRequest) = Request(request)

    /**
     * @param handler a function which returns a server response
     * @returns a server response
     */
    fun request(request: ServerRequest, handler: () -> Mono<ServerResponse>) = validator.validate(request) ?: handler()

    inner class Request(val request: ServerRequest) {
        /**
         * @param bodyType the type to deserialize
         * @param handler a function which recieves the body and returns a server response
         * @returns a server response
         */
        fun <T> withBody(bodyType: Class<T>, handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            return BodyValidator(request, bodyType).validate(handler)
        }
    }

    inner class BodyValidator<T>(val request: ServerRequest, val bodyType: Class<T>) {
        fun validate(handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            val success = { json: String -> jacksonObjectMapper().readValue(json, bodyType) }
            val json = request.body(BodyExtractors.toMono(String::class.java))
            return json.flatMap { validator.validate(request, it) ?: handler(success(it)) }
        }
    }
}
