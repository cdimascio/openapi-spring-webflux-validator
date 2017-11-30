# swagger-functional-webflux

A friendly kotlin library used to validate spring functional API endpoints against a Swagger 2.0 specification. Great with webflux functional.
It works happily with any JVM language including Java 8. 

## Prequisites

Java 8 runtime

## Install

### Maven

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>swagger-spring-functional</artifactId>
    <version>0.6.2</version>
</dependency>
```

### Gradle

```
compile 'io.github.cdimascio:swagger-spring-functional:0.6.2'
```

For sbt, grape, ivy and more, see [here](https://search.maven.org/#artifactdetails%7Cio.github.cdimascio%7Cswagger-spring-functional%7C0.6.2%7Cjar)

## Usage

### Validate a request

```kotlin
import io.github.cdimascio.swagger.Validate
```

#### Kotlin

without a body
```kotlin
Validate.request(req) {
    // Do stuff e.g. return a list of names 
    ok().body(Mono.just(listOf("carmine", "alex", "eliana")))
}
```

with body

```kotlin
Validate.request(req).withBody(User::class.java) { body ->
    // Note that body is deserialized as User!
    // Now you can do stuff. 
    // For example, lets echo the request as the response 
    ok().body(Mono.just(body))
}
```

#### Java 8

```kotlin
import io.github.cdimascio.swagger.Validate;
```

without a body
```java
ArrayList<String> users = new ArrayList<String>() {{
    add("carmine");
    add("alex");
    add("eliana");
}};

Validate.INSTANCE.request(null, () -> {
    // Do stuff e.g. return a list of user names
    ServerResponse.ok().body(fromObject(users));
});
```

with body
```java
Validate.INSTANCE
    .request(null)
    .withBody(User.class, user -> 
        // Note that body is deserialized as User!
        // Now you can do stuff. 
        // For example, lets echo the request as the response
        return ServerResponse.ok().body(fromObject(user))
    );
```


## Example


Let's say you have an endpoint `/users` that supports both `GET` and `POST` operations.

You can create those routes and validate them like so:

#### Create the routes:

```kotlin
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

#### Validate with swagger-functional-webflux
```kotlin
class UserHandler {
	
	fun findAll(req: ServerRequest): Mono<ServerResponse> {
		return Validate.request(req) {
			ok().body(Mono.just(listOf("carmine", "alex", "eliana")))
		}
	}
	 
	fun create(req: ServerRequest): Mono<ServerResponse> {
	   return Validate.request(req).withBody(User::class.java) {
	   		// it is the request body deserialized as User
			ok().body(Mono.just(it))
		}
	}
}
```

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)



