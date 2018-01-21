package io.github.cdimascio.swagger

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.net.URI
import org.junit.jupiter.api.AfterAll as afterall
import org.junit.jupiter.api.BeforeAll as beforeall
import org.junit.jupiter.api.Test as test

data class MyError(val code: Int, val name: String)
data class User(val id: Int, val name: String)

class Test {
    private val validate = Validate.configure("api.json") { status, message ->
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

        val res = validate.request(req).withBody(User::class.java) {
            fail("Validator failed to detect an inconsistency")
        }.onErrorResume {
            fail("Validator threw and unexpected error")
        }.block()
        if (res != null) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        } else {
            Assertions.fail("Failed to receive a response")
        }
    }

    @test
    fun `Validate a unsupported media type`() {
        val req = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .uri(URI.create("/api/users"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML.toString())
                .body(Mono.just("""
                    { "id": "carmine", "name": "carmine" }
                """.trimIndent()))

        val res = validate.request(req).withBody(User::class.java) {
            Assertions.assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            Assertions.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, res.statusCode())
        } else {
            Assertions.fail("Failed to receive a response")
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

        val res = validate.request(req).withBody(User::class.java) {
            Assertions.assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        } else {
            Assertions.fail("Failed to receive a response")
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

        val res = validate.request(req).withBody(User::class.java) {
            Assertions.assertNotNull(it)
            ServerResponse.ok().build()
        }.block()

        if (res != null) {
            Assertions.assertEquals(HttpStatus.OK, res.statusCode())
        } else {
            Assertions.fail("Failed to receive a response")
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
                    BodyInserters.fromObject(
                            User(1, "carmine")))
        }
    }
}

