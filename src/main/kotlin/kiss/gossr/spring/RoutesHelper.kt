package kiss.gossr.spring

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.Method
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

interface Route
interface GetRoute : Route
interface PostRoute : Route

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RouteHandler(
    val description: String = ""
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PathPrefix(
    val value: String
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PathProperty

class RoutesHelper(
    val applicationContext: ApplicationContext,
    val handlerMapping: RequestMappingHandlerMapping,
    val pathPrefix: String = "/"
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    val routes = HashMap<Class<*>, HandlerInfo>()

    class HandlerInfo(
        val requestMethods: List<RequestMethod>,
        val routeClass: Class<*>,
        val pathPrefix: String,
        val pathProperties: List<KProperty1<*, *>>,
        val paramProperties: List<KProperty1<*, *>>,
        val bean: Any,
        val method: Method,
        val annotations: Array<Annotation>
    ) {
        val binding: String get() = pathPrefix + pathProperties.joinToString("") { "/{${it.name}}" }
    }

    init {
        self = this

        applicationContext.getBeansWithAnnotation(RouteHandler::class.java).forEach { (name, bean) ->
            bean.javaClass.declaredMethods.forEach { m ->
                processMethod(bean, m)
            }
        }

        registerMappings()
    }

    private fun registerMappings() {
        routes.values.forEach { r ->
            val binding = r.binding
            log.info("${r.routeClass.simpleName} is handled on ${r.requestMethods} :: $binding")
            handlerMapping.registerMapping(
                RequestMappingInfo
                    .paths(binding)
                    .methods(*r.requestMethods.toTypedArray())
                    .build(),
                r.bean,
                r.method
            )
        }
    }

    private fun processMethod(bean: Any, m: Method) {
        val m = AopUtils.getMostSpecificMethod(m, bean.javaClass)
        if(!m.isAnnotationPresent(RouteHandler::class.java)) return
        if(m.parameters.count { Route::class.java.isAssignableFrom(it.type) } != 1) return

        val requestAndType = m.parameters.firstOrNull {
            GetRoute::class.java.isAssignableFrom(it.type) && PostRoute::class.java.isAssignableFrom(it.type)
        }?.type?.let { type ->
            listOf(RequestMethod.GET, RequestMethod.POST) to type
        } ?: m.parameters.firstOrNull { GetRoute::class.java.isAssignableFrom(it.type) }?.type?.let { type ->
            listOf(RequestMethod.GET) to type
        } ?: m.parameters.firstOrNull { PostRoute::class.java.isAssignableFrom(it.type) }?.type?.let { type ->
            listOf(RequestMethod.POST) to type
        }

        requestAndType?.let { (requestMethods, type) ->
            if(routes.contains(type)) {
                throw IllegalStateException("${type.name} route should be handled in only one method")
            }

            val constructorParams = type.kotlin.primaryConstructor?.parameters ?: emptyList()

            val properties = type.kotlin.declaredMemberProperties.sortedBy { p ->
                var index = constructorParams.indexOfFirst { it.name == p.name }
                if(index < 0) index = type.declaredFields.indexOfFirst { it.name == p.name }
                if(index < 0) Int.MAX_VALUE else index
            }.groupBy { p ->
                p.annotations.plus(constructorParams.firstOrNull { it.name == p.name }?.annotations ?: emptyList()).any { it is PathProperty }
            }

            routes.put(
                type,
                HandlerInfo(
                    requestMethods = requestMethods,
                    routeClass = type,
                    pathPrefix = getRouteUrlPathPrefix(type),
                    pathProperties = properties[true] ?: emptyList(),
                    paramProperties = properties[false] ?: emptyList(),
                    bean = bean,
                    method = m,
                    annotations = m.annotations
                )
            )
        }
    }

    private fun getRouteUrlPathPrefix(routeClass: Class<*>): String {
        routeClass.annotations.firstNotNullOfOrNull { it as? PathPrefix }?.value?.let {
            return it
        }

        return routeClass.simpleName.replace(Regex("([A-Z]+)")) { m ->
            "/${m.value.lowercase()}"
        }.removeSuffix("/route").let {
            val p = it.trimStart('/')
            val pp = pathPrefix.trim('/')
            if(pp.isEmpty()) "/$p" else "/$pp/$p"
        }
    }

    private fun getRouteUrlPath(handler: HandlerInfo, route: Route): StringBuilder {
        return StringBuilder(handler.pathPrefix).also { path ->
            handler.pathProperties.forEach { pp ->
                path.append("/").append(URLEncoder.encode(pp.getter.call(route).toString(), Charset.defaultCharset()))
            }
        }
    }

    fun getRouteUrlPath(route: Route): StringBuilder {
        return routes.get(route.javaClass)?.let { handler ->
            getRouteUrlPath(handler, route)
        } ?: throw IllegalStateException("Route ${route.javaClass} is not registered")
    }

    fun getRouteUrl(route: GetRoute): StringBuilder {
        return routes.get(route.javaClass)?.let { handler ->
            getRouteUrlPath(handler, route).also { path ->
                var n = 0
                handler.paramProperties.forEach { pp ->
                    pp.getter.call(route)?.toString()?.let { v ->
                        n++
                        path.append(if(n == 1) '?' else '&')
                            .append(pp.name)
                            .append('=')
                            .append(URLEncoder.encode(v, Charset.defaultCharset()))
                    }
                }
            }
        } ?: throw IllegalStateException("Route ${route.javaClass} is not registered")
    }

    fun getAnnotations(route: Route): Array<Annotation>? {
        return routes.get(route.javaClass)?.annotations
    }

    fun getInfo(method: Method): HandlerInfo? = routes.values.firstOrNull { it.method == method }

    companion object {
        private lateinit var self: RoutesHelper
        fun getRouteUrl(route: GetRoute): String = self.getRouteUrl(route).toString()
        fun getRouteUrlPath(route: Route): String = self.getRouteUrlPath(route).toString()
        fun getAnnotations(route: Route): Array<Annotation>? = self.getAnnotations(route)
        fun getInfo(method: Method): HandlerInfo? = self.getInfo(method)
    }
}