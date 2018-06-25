//package io.github.cdimascio.swagger
//
//import org.springframework.web.reactive.function.server.HandlerFunction
//import org.springframework.web.reactive.function.server.ServerRequest
//import org.springframework.web.reactive.function.server.ServerResponse
//import reactor.core.publisher.Mono
//
//fun validateExceptPostPutOption(request: ServerRequest, next: HandlerFunction<ServerResponse>): Mono<ServerResponse>? {
//    return when (request.methodName()) {
//        "POST", "PUT", "OPTION" -> next.handle(request)
//        else -> {
////            val error = Validator.validate(request)
////            if (error != null) ServerResponse.badRequest().body(Mono.just(error))
////            else next.handle(request)
//            val v = Validate // as Validator<*>
//            val error = v.validate(request)
//            return error?.let { it } ?: next.handle(request)
//        }
//    }
//}
