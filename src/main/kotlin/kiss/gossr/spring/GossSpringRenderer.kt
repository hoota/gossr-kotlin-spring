package kiss.gossr.spring

import kiss.gossr.DateTimeFormatEurope
import kiss.gossr.DotCommaMoneyFormat
import kiss.gossr.GossRenderer
import kiss.gossr.GossrDateTimeFormatter
import kiss.gossr.GossrMoneyFormatter
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.reflect.KClass

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

    fun <T : CssClass> classes(css: KClass<T>) = classes(CssClass.getCssClassName(css.java))
    fun <T : CssClass> classes(css: Class<T>) = classes(CssClass.getCssClassName(css))

    inline fun <T : CssClass> DIV(css: KClass<T>, body: () -> Unit = {}) = DIV(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> SPAN(css: KClass<T>, body: () -> Unit = {}) = SPAN(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> BUTTON(css: KClass<T>, body: () -> Unit = {}) = BUTTON(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> TABLE(css: KClass<T>, body: () -> Unit = {}) = TABLE(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> TH(css: KClass<T>, body: () -> Unit = {}) = TH(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> TR(css: KClass<T>, body: () -> Unit = {}) = TR(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> TD(css: KClass<T>, body: () -> Unit = {}) = TD(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> A(css: KClass<T>, body: () -> Unit = {}) = A(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> UL(css: KClass<T>, body: () -> Unit = {}) = UL(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> LI(css: KClass<T>, body: () -> Unit = {}) = LI(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> INPUT(css: KClass<T>, body: () -> Unit = {}) = INPUT(CssClass.getCssClassName(css.java), body)
    inline fun <T : CssClass> LABEL(css: KClass<T>, body: () -> Unit = {}) = LABEL(CssClass.getCssClassName(css.java), body)

    inline fun DIV(css: CssClass, body: () -> Unit = {}) = DIV(css.cssClassName, body)
    inline fun SPAN(css: CssClass, body: () -> Unit = {}) = SPAN(css.cssClassName, body)
    inline fun BUTTON(css: CssClass, body: () -> Unit = {}) = BUTTON(css.cssClassName, body)
    inline fun TABLE(css: CssClass, body: () -> Unit = {}) = TABLE(css.cssClassName, body)
    inline fun TH(css: CssClass, body: () -> Unit = {}) = TH(css.cssClassName, body)
    inline fun TR(css: CssClass, body: () -> Unit = {}) = TR(css.cssClassName, body)
    inline fun TD(css: CssClass, body: () -> Unit = {}) = TD(css.cssClassName, body)
    inline fun A(css: CssClass, body: () -> Unit = {}) = A(css.cssClassName, body)
    inline fun UL(css: CssClass, body: () -> Unit = {}) = UL(css.cssClassName, body)
    inline fun LI(css: CssClass, body: () -> Unit = {}) = LI(css.cssClassName, body)
    inline fun INPUT(css: CssClass, body: () -> Unit = {}) = INPUT(css.cssClassName, body)
    inline fun LABEL(css: CssClass, body: () -> Unit = {}) = LABEL(css.cssClassName, body)


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