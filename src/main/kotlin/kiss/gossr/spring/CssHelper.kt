package kiss.gossr.spring

import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

open class CssClass {
    lateinit var className: String

    open fun style(): String? = null
    open fun hover(): String? = null
    open fun active(): String? = null
    open fun focus(): String? = null
    open fun visited(): String? = null
    open fun firstChild(): String? = null
    open fun lastChild(): String? = null
    open fun checked(): String? = null
    open fun disabled(): String? = null
    open fun enabled(): String? = null
    open fun required(): String? = null
    open fun optional(): String? = null
    open fun empty(): String? = null
    open fun firstOfType(): String? = null
    open fun lastOfType(): String? = null
    open fun onlyChild(): String? = null
    open fun onlyOfType(): String? = null
    open fun target(): String? = null

    open fun before(): String? = null
    open fun after(): String? = null
    open fun firstLine(): String? = null
    open fun firstLetter(): String? = null
    open fun selection(): String? = null
    open fun placeholder(): String? = null
    open fun marker(): String? = null
    open fun fileSelectorButton(): String? = null

    fun additionals(): Map<String, String>? = null
}

@Component
class CssHelper(
    val applicationContext: ApplicationContext,
    val handlerMapping: RequestMappingHandlerMapping,
    val cssUrl: String = "/assets/gossr-styles-{hash}.css",
    val devMode: Boolean = false
) {
    private lateinit var classes: Map<KClass<out CssClass>, CssClass>
    private var hash: String? = null

    fun getHash(): String {
        if(devMode) return "dev"
        return hash ?: run {
            val out = StringBuilder()

            classes.values.forEach {
                outOneCssClass(out, it)
            }

            val h = DigestUtils.md5DigestAsHex(out.toString().toByteArray(Charsets.UTF_8))
            hash = h
            h
        }
    }

    fun getUrl() = cssUrl.replace("{hash}", getHash())

    @PostConstruct
    fun postConstruct() {
        instance = this

        var n = 0

        classes = applicationContext.getBeansOfType(CssClass::class.java).values.onEach { css ->
            css.className = "gossr-${n.toString(16)}"
            n++
        }.associateBy { it.javaClass.kotlin }

        handlerMapping.registerMapping(
            RequestMappingInfo.paths(cssUrl).methods(RequestMethod.GET).build(),
            this,
            this::makeFile.javaMethod
        )
    }

    fun makeFile(
        response: HttpServletResponse,
        @PathVariable hash: String
    ) {
        response.also {
            it.contentType = "text/css"

            if(devMode) {
                response.setHeader(HttpHeaders.PRAGMA, "no-cache")
                response.setHeader(HttpHeaders.EXPIRES, "0")
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
            } else {
                response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365))
                response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + (365L * 24 * 60 * 60))
            }

        }.outputStream.writer(Charsets.UTF_8).buffered(1 shl 15).use { out ->
            classes.values.forEach {
                outOneCssClass(out, it)
            }
        }
    }

    private fun outOneCssClass(out: Appendable, css: CssClass) {
        css.style()?.let { outOneCssRule(out, css.className, "", it) }
        css.hover()?.let { outOneCssRule(out, css.className, ":hover", it) }
        css.active()?.let { outOneCssRule(out, css.className, ":active", it) }
        css.focus()?.let { outOneCssRule(out, css.className, ":focus", it) }
        css.visited()?.let { outOneCssRule(out, css.className, ":visited", it) }
        css.firstChild()?.let { outOneCssRule(out, css.className, ":first-child", it) }
        css.lastChild()?.let { outOneCssRule(out, css.className, ":last-child", it) }
        css.checked()?.let { outOneCssRule(out, css.className, ":checked", it) }
        css.disabled()?.let { outOneCssRule(out, css.className, ":disabled", it) }
        css.enabled()?.let { outOneCssRule(out, css.className, ":enabled", it) }
        css.required()?.let { outOneCssRule(out, css.className, ":required", it) }
        css.optional()?.let { outOneCssRule(out, css.className, ":optional", it) }
        css.empty()?.let { outOneCssRule(out, css.className, ":empty", it) }
        css.firstOfType()?.let { outOneCssRule(out, css.className, ":first-of-type", it) }
        css.lastOfType()?.let { outOneCssRule(out, css.className, ":last-of-type", it) }
        css.onlyChild()?.let { outOneCssRule(out, css.className, ":only-child", it) }
        css.onlyOfType()?.let { outOneCssRule(out, css.className, ":only-of-type", it) }
        css.target()?.let { outOneCssRule(out, css.className, ":target", it) }

        css.before()?.let { outOneCssRule(out, css.className, "::before", it) }
        css.after()?.let { outOneCssRule(out, css.className, "::after", it) }
        css.firstLine()?.let { outOneCssRule(out, css.className, "::first-line", it) }
        css.firstLetter()?.let { outOneCssRule(out, css.className, "::first-letter", it) }
        css.selection()?.let { outOneCssRule(out, css.className, "::selection", it) }
        css.placeholder()?.let { outOneCssRule(out, css.className, "::placeholder", it) }
        css.marker()?.let { outOneCssRule(out, css.className, "::marker", it) }
        css.fileSelectorButton()?.let { outOneCssRule(out, css.className, "::file-selector-button", it) }

        css.additionals()?.forEach { (pseudo, styles) ->
            outOneCssRule(out, css.className, pseudo, styles)
        }
    }

    private fun outOneCssRule(out: Appendable, className: String, addition: String, styles: String) {
        out.append('.').append(className).append(addition).append(" {")
            .append(styles.replace('\r', ' ').replace('\n', ' '))
            .append("}\n")
    }

    companion object {

        fun <T : CssClass> getClassName(css: KClass<T>): String? =
            instance.classes[css]?.className

        lateinit var instance: CssHelper
    }
}