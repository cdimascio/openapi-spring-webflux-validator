package functional.swagger.validators

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.cdimascio.swagger.Validator
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono


object Validate {
    class BodyValidator<T>(val request: ServerRequest, val bodyType: Class<T>) {
        fun validate(handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            val success = { json: String -> jacksonObjectMapper().readValue(json, bodyType) }
            val json = request.body(BodyExtractors.toMono(String::class.java))
            return json.flatMap {
                Validator.validate(request, it)?.let {
                    ServerResponse.badRequest().body(Mono.just(it))
                } ?: handler(success(it))
            }
        }
    }


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
        val error = Validator.validate(request)
        return if (error != null) ServerResponse.badRequest().body(Mono.just(error))
        else handler()
    }

    class Request(val request: ServerRequest /* error handler but have a default */) {

        /**
         * @param bodyType the type to deserialize
         * @param handler a function which recieves the body and returns a server response
         * @returns a server response
         */
        fun <T> withBody(bodyType: Class<T>, handler: (T) -> Mono<ServerResponse>): Mono<ServerResponse> {
            return BodyValidator(request, bodyType).validate(handler)
        }
    }
}
