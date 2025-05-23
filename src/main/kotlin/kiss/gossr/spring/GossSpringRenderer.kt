package kiss.gossr.spring

import kiss.gossr.DateTimeFormatEurope
import kiss.gossr.DotCommaMoneyFormat
import kiss.gossr.GossRenderer
import kiss.gossr.GossrDateTimeFormatter
import kiss.gossr.GossrMoneyFormatter
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Suppress("FunctionNaming")
open class GossSpringRenderer : GossRenderer() {

    fun href(route: GetRoute) = attr("href", RoutesHelper.getRouteUrl(route))

    inline fun <R : Route> FORM(route: R, body: (R) -> Unit) = EL("FORM") {
        action(RoutesHelper.getRouteUrlPath(route))
        when(route) {
            is MultipartPostRoute -> {
                method("POST")
                enctype("multipart/form-data")
            }
            is PostRoute -> {
                method("POST")
            }
            is PutRoute -> {
                method("PUT")
            }
            is DeleteRoute -> {
                method("DELETE")
            }
            else -> method("GET")
        }

        body(route)

        if(route is ProtectedRoute) csrf()?.let {
            HIDDEN(it.first, it.second)
        }
    }

    companion object {
        private const val dateFormatAttributeKey = "FTGossRenderer.dateFormatAttributeKey"
        private const val moneyFormatAttributeKey = "FTGossRenderer.moneyFormatAttributeKey"

        fun request() = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        fun response() = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.response

        fun requestHost(): String? = request()?.requestURL?.replace(Regex("(https?://[^/]+)/.*"), "$1")

        fun setMoneyFormats(f: GossrMoneyFormatter?) {
            val request = request()
            request?.setAttribute(moneyFormatAttributeKey, f)
            request?.session?.setAttribute(moneyFormatAttributeKey, f)
        }

        fun getMoneyFormats(): GossrMoneyFormatter = (request()?.let { r ->
            r.getAttribute(moneyFormatAttributeKey) ?: r.session?.getAttribute(moneyFormatAttributeKey)
        } ?: DotCommaMoneyFormat) as GossrMoneyFormatter

        fun setDateTimeFormats(f: GossrDateTimeFormatter?) {
            val request = request()
            request?.setAttribute(dateFormatAttributeKey, f)
            request?.session?.setAttribute(dateFormatAttributeKey, f)
        }

        fun getDateTimeFormats(): GossrDateTimeFormatter = (request()?.let { r ->
            r.getAttribute(dateFormatAttributeKey) ?: r.session?.getAttribute(dateFormatAttributeKey)
        } ?: DateTimeFormatEurope) as GossrDateTimeFormatter

        fun isAuthenticated(): Boolean =
            request()?.userPrincipal?.name != null
    }
}