# Good Old Server Side Rendering for Kotlin

One day I got tired of fixing errors in thymeleaf templates.
I really wanted the UI-generating code to be as clear, reusable, secure, and strictly-typed as the other code in my Kotlin project. I really wanted the refactoring to not cause errors in the HTML templates. And that's how Good Old Server Sire Rendering for Kotlin came about.

```Kotlin
DIV("my-css-class") {
    classes("more css-classes")
    +"Hello World"
    BR()
    +formatDateTime(LocalDateTime.now())
}
```

## Using GOSSR views with Spring MVC

You can use it with your existing Spring MVC Controllers. Create controller and endpoint as usual and return an implementation of View or ModelAndView(Page(...))

```Kotlin
@Controller
class Controller {
    @GetMapping("/some/url")
    fun someUrlEndpoint(): View {
        // return View
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

## Using type-safe endpoints (routes)

Instead of @GetMapping/Post/PutDelete endpoint methods you can write a method annotated with @RouteHandler with a parameter that implements Route interface, for example GetRoute.

```Kotlin
import org.springframework.web.bind.annotation.PathVariable

@Controller
@RouteHandler
class Controller {

    // This endpoint will be available on /my/get/endpoint?param=World
    data class MyGetEndpointRoute(val param: String) : GetRoute

    @RouteHandler
    fun myGetEndpoint(route: MyGetEndpointRoute): View {
        return HelloWorldPage(
            text = route.param,
            time = LocalDateTime.now()
        )
    }

    // You can inherit few methods
    data class GetAndPostRoute(
        val param: String? = null,
    ) : GetRoute, PostRoute

    @RouteHandler
    fun getAndPostEndpoint(route: GetAndPostRoute): View {
        ...
    }

    // You can specify route URI
    @PathPrefix("/any/uri/here")
    class MyUriRoute : GetRoute

    // You can specify path variable
    // this will create endpoint:
    //   /my/uri/with/path/var/{id}
    data class MyUriWithPathVarRoute(
        @PathVariable
        val id: String
    ) : GetRoute
}
```

To use routes you only need to add this bean into Spring env:

```Kotlin
@Configuration
class ProjectConfiguration {
    @Bean
    fun routesHelper(
        handlerMapping: RequestMappingHandlerMapping,
        applicationContext: ApplicationContext
    ) = RoutesHelper(applicationContext, handlerMapping)
}
```

And of course you can use routes to create A-href and Forms:
```Kotlin
DIV {
    A {
        href(MyUriWithPathVarRoute("myId"))
        +"Click me"
    }
    BR()
    FORM(GetAndPostRoute(param = "Data")) { route ->
        INPUT {
            nameValueString(route::param)
        }
    }
}
```

## Using CSS with Gossr

First, make sure CssHelper bean exists in your Spring env.

```Kotlin
@Configuration
class ProjectConfiguration {
    @Bean
    fun cssHelper(
        handlerMapping: RequestMappingHandlerMapping,
        applicationContext: ApplicationContext
    ) = CssHelper(
        applicationContext, 
        handlerMapping,
        // use devMode = true to disable CSS file caching and enable JVM hot-reload support
        // make sure devMode == false in production
        devMode = System.getProperty("dev-mode") == "true"
    )
}
```

and link CSS file somehere in your &lt;head&gt;...&lt;/head&gt; element
```Kotlin
LINK(rel = "stylesheet", href = CssHelper.instance.getUrl())
```

Create Spring @Component of CSS-Class
```Kotlin
@Component
object MyCssClass : CssClass({
    style = "color: red"
    hover = "text-decoration: underline;"
})
```
And use it in your code
```Kotlin
DIV(MyCssClass) {
    +"Hello World"
}
```


To see more examples, please, check out [Gossr for Kotlin and Spring Examples](https://github.com/hoota/gossr-kotlin-spring-examples).