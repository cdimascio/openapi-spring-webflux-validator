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
    <version>0.7.1</version>
</dependency>
```

### Gradle

```
compile 'io.github.cdimascio:swagger-spring-functional:0.7.1'
```

For sbt, grape, ivy and more, see [here](https://search.maven.org/#artifactdetails%7Cio.github.cdimascio%7Cswagger-spring-functional%7C0.7.1%7Cjar)

## Usage (Kotlin)

The following sections describe usage. The first section shows using with Kotlin. The second section show usage with Java 8.

### Configure

One time configuration, must specify the location of the swagger specification and may optionally provide a custom error handler/

```kotlin
import io.github.cdimascio.swagger.Validate
val validate = Validate.configure("static/api.json")
```

with custom error handler

```kotlin
data class MyError(val id: String, val messages: List<String>)
val validate = Validate.configure("static/api.json") { messages ->
   Error("my_error_handler", messages)
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

#### Java 8

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
    public getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public getMessages() {
        return messages;
    }
    public void setMessages(List<String> messages) {
        this.messages = messages;
    }     
}
```

```java
Validate<ValidationError> validate = Validate.configure("static/api.json", messages ->
    new MyError("my_error_handler", messages)
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



