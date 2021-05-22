package io.github.cdimascio.openapi

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import reactor.core.publisher.Mono
import java.net.URI
import org.junit.jupiter.api.Test as test

class CoroutinesTest {
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

        val res = runBlocking {
            validate.request(req).awaitBody(User::class.java) {
                fail("Validator failed to detect an inconsistency")
            }
        }

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
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

        val res = runBlocking {
            validate.request(req).awaitBody(User::class.java) {
                assertNotNull(it)
                ServerResponse.ok().buildAndAwait()
            }
        }

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, res.statusCode())
    }

    @test
    fun `Validate an empty body`() {
        val req = MockServerRequest.builder()
            .method(HttpMethod.POST)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .uri(URI.create("/api/users"))
            .body(Mono.empty<String>())

        val res = runBlocking {
            validate.request(req).awaitBody(User::class.java) {
                assertNotNull(it)
                ServerResponse.ok().buildAndAwait()
            }
        }

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
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

        val res = runBlocking {
            validate.request(req).awaitBody(User::class.java) {
                assertNotNull(it)
                ServerResponse.ok().buildAndAwait()
            }
        }

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
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

        val res = runBlocking {
            validate.request(req).awaitBody(User::class.java) {
                assertNotNull(it)
                ServerResponse.ok().buildAndAwait()
            }
        }

        assertEquals(HttpStatus.OK, res.statusCode())
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
        val res = runBlocking {
            validate.request(req).awaitBody(String::class.java, readValue = identity) {
                assertEquals(body, it)
                ServerResponse.ok().buildAndAwait()
            }
        }

        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @test
    fun `Validate a get request`() {
        val req = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create("/api/users"))
            .build()
        runBlocking {
            validate.requestAndAwait(req) {
                ServerResponse.ok()
                    .bodyValueAndAwait(User(1, "carmine"))
            }
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

        val res = runBlocking {
            validate.request(req).awaitBody(User::class.java) {
                assertNotNull(it)
                ServerResponse.ok().buildAndAwait()
            }
        }

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, res.statusCode())
    }
}
