package io.github.cdimascio.openapi

import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.net.URI
import org.junit.jupiter.api.Test as test



class ReactiveTest {
    private val validate = Validate.configure("api.yaml") { status, message ->
        MyError(status.value(), message[0])
    }

    @test
    fun `Validate an invalid post request`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .uri(URI.create("/api/users"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(Mono.just("""
                    { "bad_key": 1, "name": "carmine" }
                """.trimIndent()))

        val res = validate.request(req).withBody<User> {
            fail("Validator failed to detect an inconsistency")
        }.onErrorResume {
            fail("Validator threw and unexpected error")
        }.block()
        if (res != null) {
            assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate a unsupported media type`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .uri(URI.create("/api/users"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(Mono.just("""
                    { "id": "carmine", "name": "carmine" }
                """.trimIndent()))

        val res = validate.request(req).withBody<User> {
            assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate an empty body`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .uri(URI.create("/api/users"))
                .body(Mono.empty<String>())

        val res = validate.request(req).withBody<User> {
            assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate bad value type returns bad request`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .uri(URI.create("/api/users"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(Mono.just("""
                    { "id": "should_be_a_number", "name": "dimascio" }
                """.trimIndent()))

        val res = validate.request(req).withBody<User> {
            assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate a post request`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .uri(URI.create("/api/users"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(Mono.just("""
                    { "id": 1, "name": "dimascio" }
                """.trimIndent()))

        val res = validate.request(req).withBody<User> {
            assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            assertEquals(HttpStatus.OK, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate a post request and provide access to string body`() {
        val body = """{ "id": 1, "name": "dimascio" }"""
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .uri(URI.create("/api/users"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(Mono.just(body))

        val identity: (String) -> String = { it }
        val res = validate.request(req).withBody(String::class.java, readValue = identity) {
            assertEquals(body, it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            assertEquals(HttpStatus.OK, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate a get request`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.GET)
                .uri(URI.create("/api/users"))
                .build()
        validate.request(req) {
            ServerResponse.ok().body(
                    BodyInserters.fromValue(
                            User(1, "carmine")))
        }
    }

    @test
    fun `Validate method not allowed`() {
        val req = MockServerRequest.builder()
            .method(HttpMethod.PUT)
            .uri(URI.create("/api/users"))
            .body(Mono.just("""
                    { "id": 1, "name": "dimascio" }
                """.trimIndent()))

        val res = validate.request(req).withBody<User> {
            assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, res.statusCode())
        } else {
            fail("Failed to receive a response")
        }
    }
}
