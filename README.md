# Good Old Server Side Rendering for Kotlin

One day I got tired of fixing errors in thymeleaf templates.
I really wanted the UI-generating code to be as clear, reusable, secure, and strictly-typed as the other code in my Kotlin project. I really wanted the refactoring to not cause errors in the HTML templates. And that's how Good Old Server Sire Rendering for Kotlin came about.

```Kotlin
DIV {
    classes("my-css-class")
    +"Hello World"
    BR()
    +formatDateTime(LocalDateTime.now())
}
```

You can use it with your existing Spring MVC Controllers

```Kotlin
@Controller
class Controller {
    @GetMapping("/some/url")
    fun someUrlEndpoint(): View {
        return HelloWorldPage(
            text = "world",
            time = LocalDateTime.now()
        )
    }
}

class HelloWorldPage(
    val text: String,
    val time: LocalDateTime
) : GossSpringRenderer(), GossrSpringView {
    override fun draw() {
        DIV {
            +"Hello $text"
            BR()
            formatDateTime(time)
        }
    }
}
```

Or you can use type-safe endpoints:
```Kotlin
@Controller
class Controller {

    // This endpoint will be available on /my/get/endpoint?param=World
    data class MyGetEndpointRoute(val param: String): GetRoute

    @RouteHandler
    fun myGetEndpoint(route: MyGetEndpointRoute): View {
        return HelloWorldPage(
            text = route.param,
            time = LocalDateTime.now()
        )
    }
}

// only need to add this bean into Spring

@Configuration
class ProjectConfiguration {
    @Bean
    fun routesHelper(
        handlerMapping: RequestMappingHandlerMapping,
        applicationContext: ApplicationContext
    ) = RoutesHelper(applicationContext, handlerMapping)
}
```

To see more examples, please, check out [Gossr for Kotlin and Spring Examples](https://github.com/hoota/gossr-kotlin-spring-examples).