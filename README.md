# GOSSR for Kotlin & Spring

**GOSSR for Kotlin & Spring** is an integration module that connects **[GOSSR Core](https://github.com/hoota/gossr-kotlin)**
(pure Kotlin HTML rendering) with **Spring MVC**.

It allows you to build **fully server-side rendered web applications** using:
- strictly typed routes
- Kotlin-based views
- refactor-safe links and forms
- optional CSS lifecycle management
- and early validation of form–backend contracts

This module intentionally builds on top of Spring MVC instead of replacing it.

---

## What problems does it solve?

### ❌ String-based routing and URLs

Classic Spring MVC relies heavily on string literals:

- `@GetMapping("/users/{id}")`
- `"redirect:/some/url"`
- hardcoded links in templates

These break silently during refactoring.

**GOSSR routes are types**, not strings:
- URLs are derived from route classes
- parameters are constructor arguments
- refactoring is safe by default

---

### ❌ Fragile HTML templates

Template engines introduce:
- a second language
- weak typing
- runtime-only failures
- limited IDE support

With GOSSR:
- HTML is Kotlin code
- views are regular classes
- full IDE navigation and refactoring works
- using all Kotlin features to 
- **JVM Hot-Reload support** in Debug mode — in most cases you don't need to restart your app to update HTML

---

### ❌ Broken forms discovered too late

A common and painful problem:

> The form renders fine,  
> the user fills it in,  
> presses Submit — and the backend rejects it.

GOSSR for Spring can **verify form integrity at render time**:
- required route parameters must be represented in the form
- missing fields are detected *before* user interaction
- no reliance on submit-time surprises

### Why Kotlin for UI rendering?

Using Kotlin as the language for UI rendering means the full power of the language is available when building pages.

You can use constants, collection iteration, ordinary functions that render reusable page fragments, and shared code exactly the same way you reuse business logic. UI blocks can be extracted into functions or base classes, extended or overridden through inheritance, and composed without introducing a separate template language. Control flow is expressed using normal `if / else`, `when`, `try / catch`, and loops — not limited or ad-hoc DSL constructs. As a result, UI code remains readable, testable, and refactor-friendly even as it grows in complexity.

### Fast feedback and hot reload

In many cases, updating a page does not require restarting the application.

Because views are plain Kotlin code executed on the JVM, a large class of UI changes can be applied using JVM hot reload during development. This provides fast feedback loops similar to template editing, while keeping all the benefits of strong typing and IDE support. Full application restarts are needed only when changing class signatures (adding/deleting/replacing fields, classes or functions)

---

## Using GOSSR views with Spring MVC

Controllers remain ordinary Spring MVC controllers.

```kotlin
@Controller
class Controller {

    @GetMapping("/hello")
    fun hello(): View =
        HelloWorldPage(
            text = "world",
            time = LocalDateTime.now()
        )
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

---

## Type-safe routes instead of @GetMapping strings

Routes are expressed as Kotlin classes:

```kotlin
data class MyGetRoute(
    val param: String
) : GetRoute
```

And handled using `@RouteHandler`:

```kotlin
@Controller // or @Component - this class just needs to be a Spring Bean
@RouteHandler
class Controller {

    @RouteHandler
    // this will created GET::/my/get endpoint
    fun myEndpoint(route: MyGetRoute): View {
        return HelloWorldPage(
            text = route.param,
            time = LocalDateTime.now()
        )
    }
}
```

URLs are generated from route instances:

```kotlin
A {
    // this will give "/my/get?param=World" URL
    href(MyGetRoute("World"))
    +"Click me"
}
```

You can also override URL Prefix with `@PathPrefix`:
```kotlin
@PathPrefix("/any/uri/here")
class MyUriRoute : GetRoute
```
or make some properties to be placed into path itself:
```kotlin
data class MyUriWithPathVarRoute(
    @PathVariable
    val id: String
) : GetRoute // = "/my/uri/with/path/var/{id}"
```
---

## Form rendering and route-backed forms

Forms are rendered from route instances:

```kotlin
FORM(MyGetRoute(param = "initial")) { route ->
    INPUT {
        nameValueString(route::param)
    }
}
```

This guarantees:
- correct parameter names
- correct types
- refactor-safe binding

---

## Form–Route contract validation (important)

This module introduces **form contract validation**.

### The problem

It is easy to forget to include a required route parameter in a form,
especially when the field is hidden, nested, or conditional.

### The solution

When rendering a form, GOSSR can:

- collect all field names rendered inside the form
- compare them with required route properties
- detect missing fields *during rendering*

Required form properties are determined using Kotlin semantics:

- non-null constructor parameters without default values → required
- nullable or defaulted parameters → optional
- `@ForceFormFieldPresentCheck` → always required

Nested objects, lists and maps are also supported:
- `property[...]`
- `property.subfield`

### Handling missing fields

When a mismatch is detected, the framework calls:

```kotlin
open fun <R : Route> onRoutePropertyIsMissingInForm(
    route: R,
    missingProperties: List<KProperty1<*, *>>
)
```

The default implementation does nothing.

Application code may override this hook to:
- disable the form
- render an error banner
- log or report the issue
- notify developers (Slack, Sentry, etc.)
- fail fast in development
- play a loud sound from browser in dev mode :)

This keeps UX and operational behavior **fully application-defined**.

---

## CSS lifecycle support (optional)

This module also provides optional CSS lifecycle management.

CSS classes can be defined as Spring components:

```kotlin
@Component
object MyCssClass : CssClass({
    style = "color: red"
    hover = "text-decoration: underline"

    add(">a", "color: green;")

    media("max-width: 991px") {
        style = "color: green;"
    }
})
```

Used directly in views:

```kotlin
HEAD {
    LINK(rel = "stylesheet", href = CssHelper.instance.getUrl())
}

...

DIV(MyCssClass) {
    +"Hello World"
}
```

Benefits:
- styles live next to the code that uses them
- unused CSS is easy to detect and remove
- responsive breakpoints can be Kotlin constants
- long-lived projects avoid CSS entropy
- **JVM Hot-Reload support** in Debug mode — generates new CSS file with new name on request

---

## What this module IS

- ✔️ Spring MVC integration for GOSSR Core
- ✔️ Typed routing and URL generation
- ✔️ Refactor-safe forms and links
- ✔️ Early detection of broken form contracts
- ✔️ Server-side rendering first

---

## What this module is NOT

- ❌ Not a replacement for Spring MVC
- ❌ Not a frontend framework
- ❌ Not a SPA or reactive UI system
- ❌ Not a template engine
- ❌ Not opinionated about UX or styling

---

## Design philosophy

> Make invalid states unrepresentable —  
> or at least detectable *before the user sees them*.

GOSSR for Spring focuses on:
- early failure
- explicit contracts
- predictable behavior
- long-term maintainability

---

## Setup

Add dependency (Maven for example):
```XML
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

...

<dependency>
    <groupId>com.github.hoota</groupId>
    <artifactId>gossr-kotlin-spring</artifactId>
    <version>0.5.7</version>
</dependency>
```

Register required infrastructure beans:

```kotlin
@Configuration
class ProjectConfiguration {

    @Bean
    fun routesHelper(
        handlerMapping: RequestMappingHandlerMapping,
        applicationContext: ApplicationContext
    ) = RoutesHelper(applicationContext, handlerMapping)

    // Optional
    @Bean
    fun cssHelper(
        handlerMapping: RequestMappingHandlerMapping,
        applicationContext: ApplicationContext
    ) = CssHelper(applicationContext, handlerMapping)
}
```

---

## Relationship to GOSSR Core

- **[GOSSR Core](https://github.com/hoota/gossr-kotlin)**: pure Kotlin HTML rendering, no dependencies, framework-agnostic
- **GOSSR Spring**: Spring MVC integration, routing, forms, CSS lifecycle

Use Core alone if you want rendering only.  
Use Spring module when building full web applications.

---

## License

MIT
