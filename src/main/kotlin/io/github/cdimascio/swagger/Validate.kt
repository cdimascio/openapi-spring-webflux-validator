package io.github.cdimascio.swagger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono


data class ValidationError(val code: Int, val message: String)

class Validate<out T> internal constructor(swaggerJsonPath: String, errorHandler: (List<String>) -> T) {
    companion object Instance {
        private var defaultErrorHandler = { messages: List<String> ->
            ValidationError(400, messages[0])
        }

        fun  configure(
                swaggerJsonPath: String
        ): Validate<ValidationError> {
            return Validate(swaggerJsonPath, defaultErrorHandler)
        }

        fun  <T> configure(
                swaggerJsonPath: String,
                errorHandler: (List<String>) -> T
        ): Validate<T> {
            return Validate(swaggerJsonPath, errorHandler)
        }
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
    fun request(request: ServerRequest, handler: () -> Mono<ServerResponse>): Mono<ServerResponse> {
        val error = validator.validate(request)
        return error?.let { it } ?: handler()
    }

    inner class Request(val request: ServerRequest /* error handler but have a default */) {
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
            return json.flatMap { validator.validate(request, it)?.let { it } ?: handler(success(it)) }
        }
    }
}
