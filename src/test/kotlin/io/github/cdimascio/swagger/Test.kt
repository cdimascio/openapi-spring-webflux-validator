//package io.github.cdimascio.swagger
//
//import org.springframework.web.reactive.function.server.ServerRequest
//import org.junit.BeforeClass as beforeall
//import org.springframework.web.reactive.function.server.ServerResponse
//import org.junit.Test as test
//
//class Test {
//    var defaultValidator: Validate<ValidationError>? = null
//    @beforeall
//    fun beforeAll() {
//        defaultValidator = Validate.configure("api.json")
////        customValidator = Validate.configure("api.json")
//    }
//
//	@test fun `Validate a post request`() {
//        defaultValidator?.let {
//            it.request()
//        }
////        Validate.request(null, {
////            // Do stuff e.g. return a list of names
////            ServerResponse.ok().bodyType(fromObject<ArrayList<String>>(list))
////        })
//
//
//        Validate.requestuest(null).withBody(BIConversion.User::class.java, { user -> ServerResponse.ok().body(fromObject<T>(user)) })
//	}
//}