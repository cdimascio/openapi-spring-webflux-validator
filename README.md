# swagger-functional-webflux

![](https://img.shields.io/badge/build-passing-green.svg)![](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

A friendly kotlin library used to validate spring functional API endpoints against a Swagger 2.0 specification. Great with webflux functional. 
It **works happily with any JVM language including Java 8**. 

![](https://raw.githubusercontent.com/cdimascio/swagger-spring-functional/master/assets/swagger.png)
![](https://raw.githubusercontent.com/cdimascio/swagger-spring-functional/master/assets/spring5.png)

## Prequisites

Java 8 runtime

## Install

### Maven

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>swagger-spring-functional</artifactId>
    <version>0.8.0</version>
</dependency>
```

### Gradle

```
compile 'io.github.cdimascio:swagger-spring-functional:0.8.0'
```

For sbt, grape, ivy and more, see [here](https://search.maven.org/#artifactdetails%7Cio.github.cdimascio%7Cswagger-spring-functional%7C0.8.0%7Cjar)

## Usage (Kotlin)

The following sections describe usage. The first section shows using with Kotlin. The second section show usage with Java 8.

### Configure

One time configuration, must specify the location of the swagger specification and may optionally provide a *custom error handler!*

```kotlin
import io.github.cdimascio.swagger.Validate
val validate = Validate.configure("static/api.json")
```

with custom error handler

```kotlin
data class MyError(val id: String, val messages: List<String>)
val validate = Validate.configure("static/api.json") { status, messages ->
   Error(status.name, messages)
}
```

### Validate a request

Using the `validate` instance created above, you can now validate a request:

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

## Usages (Java 8)

### Configure
One time configuration, must specify the location of the swagger specification and may optionally provide a custom error handler/

```java
import io.github.cdimascio.swagger.Validate;
Validate<ValidationError> validate = Validate.configure("static/api.json")
```

with custom error handler

```java
class MyError {
    private String id;
    private  String messages;
    public MyError(String id, List<String> messages) {
        this.id = id;
        this.messages = messages;
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
Validate<ValidationError> validate = Validate.configure("static/api.json", (status, messages) ->
    new MyError(status.getName(), messages)
);
```

### Validate a request

Using the `validate` instance created above, you can now validate a request:

without a body

```java
ArrayList<String> users = new ArrayList<String>() {{
    add("carmine");
    add("alex");
    add("eliana");
}};

validate.request(null, () -> {
    // Do stuff e.g. return a list of user names
    ServerResponse.ok().body(fromObject(users));
});
```

with body

```java
validate
    .request(null)
    .withBody(User.class, user -> 
        // Note that body is deserialized as User!
        // Now you can do stuff. 
        // For example, lets echo the request as the response
        return ServerResponse.ok().body(fromObject(user))
    );
```

## Example Valiation Output

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

`swagger-functional-webflux` automatically validates the request against a Swagger spect and returns:

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

#### Create the routes:

```kotlin
package myproject.controllers

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.ServerResponse.permanentRedirect
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
    }
}
```

```kotlin
package myproject

import io.github.cdimascio.swagger.Validate
val validate = Validate.configure("static/api.json")
```

#### Validate with swagger-functional-webflux
```kotlin
package myproject.controllers

import myproject.models.User
import myproject.validate
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserHandler {
	
	fun findAll(req: ServerRequest): Mono<ServerResponse> {
		return validate.request(req) {
			ok().body(Mono.just(listOf("carmine", "alex", "eliana")))
		}
	}
	 
	fun create(req: ServerRequest): Mono<ServerResponse> {
	   return validate.request(req).withBody(User::class.java) {
	   		// it is the request body deserialized as User
			ok().body(Mono.just(it))
		}
	}
}
```

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)



