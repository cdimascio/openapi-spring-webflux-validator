# openapi-spring-webflux-validator
![](https://travis-ci.org/cdimascio/openapi-spring-webflux-validator.svg?branch=master)[![Maven Central](https://img.shields.io/maven-central/v/io.github.cdimascio/openapi-spring-webflux-validator.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.cdimascio%22%20AND%20a:%22openapi-spring-webflux-validator%22) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/f78b72ca90104e42b111723a7720adf3)](https://www.codacy.com/app/cdimascio/openapi-spring-webflux-validator?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cdimascio/openapi-spring-webflux-validator&amp;utm_campaign=Badge_Grade) ![](https://img.shields.io/badge/license-Apache%202.0-blue.svg)<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-6-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END --> 

A friendly kotlin library to validate API endpoints using an _OpenApi 3_ or _Swagger 2_ specification. Great with webflux functional. 
It **works happily with any JVM language including Java >=8**. 
<p align="center">
	<img src="https://raw.githubusercontent.com/cdimascio/openapi-spring-webflux-validator/master/assets/openapi-spring5-webflux-validator.png" width="600"/>
</p>

Supports specifications in _YAML_ and _JSON_

See this [complete Spring 5 Webflux example that uses openapi-spring-webflux-validator](https://github.com/cdimascio/kotlin-swagger-spring-functional-template).

## Prequisites

Java 8 or greater

## Install

### Maven

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>openapi-spring-webflux-validator</artifactId>
    <version>4.1.0</version>
</dependency>
```

### Gradle

```groovy
compile 'io.github.cdimascio:openapi-spring-webflux-validator:4.1.0'
```

For sbt, grape, ivy and more, see [here](https://search.maven.org/#artifactdetails%7Cio.github.cdimascio%7Copenapi-spring-webflux-validator%7C2.0.0%7Cjar)

## Usage (Kotlin)

This section and the next describe usage with Kotlin and Java respectively.

### Configure (Kotlin)

This one-time configuration requires you to provide the _location of the openapi/swagger specification_ and an optional _custom error handler_.

Supports `JSON` and `YAML`

```kotlin
import io.github.cdimascio.openapi.Validate

val validate = Validate.configure("static/api.yaml")
```

with custom error handler

```kotlin
import org.springframework.web.reactive.function.server.ServerRequest

data class MyError(val request: ServerRequest, val code: String, val messages: List<String>)
val validate = Validate.configure("static/api.json") { request, status, messages ->
   MyError(request, status.name, messages)
}
```

with custom ObjectMapper factory:

```kotlin
val validate = Validate.configure(
   openApiSwaggerPath = "api.yaml",
   errorHandler = { request, status, message -> ValidationError(request, status.value(), message[0]) },
   objectMapperFactory = { ObjectMapper()
       .registerKotlinModule()
       .registerModule(JavaTimeModule())
       .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false) }
)
```

### Validate a request (Kotlin + Reactor)

You can now validate a request in a coroutine style,
using the `validate` instance created [above](#configure-kotlin):

without a body

```kotlin
validate.request(req) {
    // Do stuff e.g. return a list of names 
    ok().body(Mono.just(listOf("carmine", "alex", "eliana")))
}
```

with body

```kotlin
validate.request(req).withBody(User::class.java) { body ->
    // Note that body is deserialized as User!
    // Now you can do stuff. 
    // For example, lets echo the request as the response 
    ok().body(Mono.just(body))
}
```

with body you want to process as string (e.g. for computing a request signature), or that you want to deserialize somehow specifically

```kotlin
val identity: (String) -> String = { it }
validate.request(req).withBody(String::class.java, readValue = identity) { body ->
    ok().body(Mono.just("content length is ${body.length}"))
}
```

### Validate a request (Kotlin + coroutines)

Or you can validate a request in a coroutine style,
using the `validate` instance created [above](#configure-kotlin):


without a body

```kotlin
validate.requestAndAwait(req) {
    // Do stuff e.g. return a list of names 
    ok().bodyValueAndAwait(listOf("carmine", "alex", "eliana"))
}
```

with body

```kotlin
validate.request(req).awaitBody(User::class.java) { body: User ->
    // Note that body is deserialized as User!
    // Now you can do stuff. 
    // For example, lets echo the request as the response 
    ok().bodyValueAndAwait(body)
}
```

with body you want to process as string (e.g. for computing a request signature), or that you want to deserialize somehow specifically

```kotlin
val identity: (String) -> String = { it }
validate.request(req).awaitBody(String::class.java, identity) { body: String ->
    ok().bodyValueAndAwait("content length is ${body.length}")
}
```

## Usage (Java 8 _or greater_)

### Configure (Java)
This one-time configuration requires you to provide the _location of the openapi/swagger specification_ and an optional _custom error handler_.

```java
import io.github.cdimascio.openapi.Validate;

Validate<ValidationError> validate = Validate.configure("static/api.json")
```

with custom error handler

```java
import org.springframework.web.reactive.function.server.ServerRequest;

class MyError {
    private ServerRequest request;
    private String id;
    private  String messages;
    public MyError(ServerRequest request, String id, List<String> messages) {
        this.request = request;
        this.id = id;
        this.messages = messages;
    }
    public ServerRequest getRequest() {
        return request;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public List<String> getMessages() {
        return messages;
    }
    public void setMessages(List<String> messages) {
        this.messages = messages;
    }     
}
```

```java
Validate<ValidationError> validate = Validate.configure("static/api.json", (request, status, messages) ->
    new MyError(request, status.getName(), messages)
);
```

### Validate a request (Java)

Using the `validate` instance created above, you can now validate a request:

without a body

```java
ArrayList<String> users = new ArrayList<String>() {{
    add("carmine");
    add("alex");
    add("eliana");
}};

validate.request(req, () ->
    // Do stuff e.g. return a list of user names
    ServerResponse.ok().bodyValue(users)
);
```

with body

```java
validate
    .request(req)
    .withBody(User.class, user -> 
        // Note that body is deserialized as User!
        // Now you can do stuff. 
        // For example, lets echo the request as the response
        ServerResponse.ok().bodyValue(user)
    );
```

with body you want to process as string (e.g. for computing a request signature)

```java
validate
    .request(req)
    .withBody(String.class, s -> s, body ->
        ServerResponse.ok().bodyValue("content length is " + body.length())
    );
```

## Example Validation Output

Let's assume a `POST` request to create a user requires the following request body:

```json
{
  "firstname": "carmine",
  "lastname": "dimasico"
}
```

Let's now assume an API user misspells `lastname` as `lastnam`

```shell
curl -X POST http://localhost:8080/api/users -H "Content-Type: application/json" -d'{ 
  "firstname": "c", 
  "lastnam": "d" 
}'
```

`openapi-spring-webflux-validator` automatically validates the request against a Swagger spect and returns:

```json
{
  "code": 400,
  "messages":[
	  "Object instance has properties which are not allowed by the schema: [\"lastnam\"]",
	  "Object has missing required properties ([\"lastname\"])"
  ]
} 
```

**Woah! Cool!!** :-D 

## Example

Let's say you have an endpoint `/users` that supports both `GET` and `POST` operations.

You can create those routes and validate them like so:

**Create the routes in a reactive or coroutine style:**

```kotlin
package myproject.controllers

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.ServerResponse.permanentRedirect
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.plus
import org.springframework.web.reactive.function.server.router
import java.net.URI

class Routes(private val userHandler: UserHandler) {
    fun router() = router {
        "/api".nest {
            accept(APPLICATION_JSON).nest {
                POST("/users", userHandler::create)
            }
            accept(TEXT_EVENT_STREAM).nest {
                GET("/users", userHandler::findAll)
            }
        }
    } + coRouter { 
        "/coApi".nest {
            accept(APPLICATION_JSON).nest {
                POST("/users", userHandler::coCreate)
            }
            accept(TEXT_EVENT_STREAM).nest {
                GET("/users", userHandler::coFindAll)
            }
        }
    }
}
```

```kotlin
package myproject

import io.github.cdimascio.openapi.Validate

val validate = Validate.configure("static/api.yaml")
```

**Validate with openapi-spring-webflux-validator**

```kotlin
package myproject.controllers

import myproject.models.User
import myproject.validate
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserHandler {

    fun findAll(req: ServerRequest): Mono<ServerResponse> {
        return validate.request(req) {
            ok().bodyValue(listOf("carmine", "alex", "eliana"))
        }
    }

    fun create(req: ServerRequest): Mono<ServerResponse> {
        return validate.request(req).withBody(User::class.java) {
            // it is the request body deserialized as User
            ok().bodyValue(it)
        }
    }

    suspend fun coFindAll(req: ServerRequest): ServerResponse {
        return validate.requestAndAwait(req) {
            ok().bodyValueAndAwait(listOf("carmine", "alex", "eliana"))
        }
    }

    suspend fun coCreate(req: ServerRequest): ServerResponse {
        return validate.request(req).awaitBody(User::class.java) {
            // it is the request body deserialized as User
            ok().bodyValueAndAwait(it)
        }
    }
}
```

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

<a href="https://www.buymeacoffee.com/m97tA5c" target="_blank"><img src="https://bmc-cdn.nyc3.digitaloceanspaces.com/BMC-button-images/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: auto !important;width: auto !important;" ></a>

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://github.com/cdimascio"><img src="https://avatars1.githubusercontent.com/u/4706618?v=4" width="100px;" alt=""/><br /><sub><b>Carmine DiMascio</b></sub></a><br /><a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=cdimascio" title="Code">💻</a> <a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=cdimascio" title="Tests">⚠️</a> <a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=cdimascio" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/krzykrucz"><img src="https://avatars1.githubusercontent.com/u/18364177?v=4" width="100px;" alt=""/><br /><sub><b>Krzysiek Kruczyński</b></sub></a><br /><a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=krzykrucz" title="Code">💻</a> <a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=krzykrucz" title="Tests">⚠️</a> <a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=krzykrucz" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/chejerlakarthik"><img src="https://avatars0.githubusercontent.com/u/12871079?v=4" width="100px;" alt=""/><br /><sub><b>Chejerla Karthik</b></sub></a><br /><a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=chejerlakarthik" title="Code">💻</a></td>
    <td align="center"><a href="http://www.katielevy.com"><img src="https://avatars0.githubusercontent.com/u/8975181?v=4" width="100px;" alt=""/><br /><sub><b>Katie Levy</b></sub></a><br /><a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=katielevy1" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/reinterpretcat"><img src="https://avatars1.githubusercontent.com/u/1611077?v=4" width="100px;" alt=""/><br /><sub><b>Ilya Builuk</b></sub></a><br /><a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=reinterpretcat" title="Code">💻</a></td>
    <td align="center"><a href="http://simon.zambrovski.org/"><img src="https://avatars0.githubusercontent.com/u/673128?v=4" width="100px;" alt=""/><br /><sub><b>Simon Zambrovski</b></sub></a><br /><a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=zambrovski" title="Code">💻</a> <a href="https://github.com/cdimascio/openapi-spring-webflux-validator/commits?author=zambrovski" title="Tests">⚠️</a></td>
  </tr>
</table>

<!-- markdownlint-enable -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
