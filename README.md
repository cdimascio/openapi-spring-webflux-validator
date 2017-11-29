# swagger-funtional-webflux

A friendly kotlin library used to validate API endpoints against a Swagger 2.0 specification

## Usage


### Validate

```kotlin
Validate.request(req) {
    ok().body(Mono.just(listOf("carmine", "alex", "eliana")))
}
```

#### with body

```kotlin
Validate.request(req).withBody(User::class.java) { 
	body -> ok().body(Mono.just(body)) // body is deserialized as User
}
```


## Example


Let's say you function to create `/users`

#### Set up routes:

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

#### validate handler functions
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

### License
Apache 2.0



