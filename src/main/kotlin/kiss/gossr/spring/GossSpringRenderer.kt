package kiss.gossr.spring

import kiss.gossr.DateTimeFormatEurope
import kiss.gossr.DotCommaMoneyFormat
import kiss.gossr.GossRenderer
import kiss.gossr.GossrDateTimeFormatter
import kiss.gossr.GossrMoneyFormatter
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Suppress("FunctionNaming")
open class GossSpringRenderer : GossRenderer() {

    override fun csrf(): Pair<String, String>? = (request()?.getAttribute("_csrf") as? CsrfToken)?.let {
        it.parameterName to it.token
    }

    fun href(route: GetRoute) = attr("href", RoutesHelper.getRouteUrl(route))

    inline fun <R : GetRoute> FORM(route: R, fullUrl: Boolean = false, body: (R) -> Unit) = EL("FORM") {
        action(if(fullUrl) RoutesHelper.getRouteUrl(route) else RoutesHelper.getRouteUrlPath(route))
        method("GET")
        body(route)
    }

    inline fun <R : PostRoute> FORM(route: R, enctype: String? = null, body: (R) -> Unit) = EL("FORM") {
        enctype(enctype)
        action(RoutesHelper.getRouteUrlPath(route))
        method("POST")
        body(route)
        csrf()?.let {
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