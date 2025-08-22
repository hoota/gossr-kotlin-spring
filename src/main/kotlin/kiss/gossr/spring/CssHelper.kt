package kiss.gossr.spring

import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.BufferedWriter
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

open class CssStyles {
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

open class CssClass : CssStyles() {
    lateinit var generatedCssClassName: String

    open fun cssClassName(): String = generatedCssClassName

    open fun medias(): Map<String, CssStyles>? = null
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
                outOneCssClass(out, it.cssClassName(), it)
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
            css.generatedCssClassName = "gossr-${n.toString(16)}"
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
            val medias = HashMap<String, ArrayList<Pair<String, CssStyles>>>()

            classes.values.forEach {
                outOneCssClass(out, it.cssClassName(), it)
                it.medias()?.forEach { (media, style) ->
                    medias.computeIfAbsent(media) { ArrayList() }.add(it.cssClassName() to style)
                }
            }

            medias.forEach { (media, styles) ->
                appendMedia(out, media, styles)
            }
        }
    }

    private fun appendMedia(out: BufferedWriter, media: String, styles: java.util.ArrayList<Pair<String, CssStyles>>) {
        out.append("@media(").append(media).append(") {\n")
        styles.forEach { outOneCssClass(out, it.first, it.second) }
        out.append("}\n")
    }

    private fun outOneCssClass(out: Appendable, className: String, styles: CssStyles) {
        styles.style()?.let { outOneCssRule(out, className, "", it) }
        styles.hover()?.let { outOneCssRule(out, className, ":hover", it) }
        styles.active()?.let { outOneCssRule(out, className, ":active", it) }
        styles.focus()?.let { outOneCssRule(out, className, ":focus", it) }
        styles.visited()?.let { outOneCssRule(out, className, ":visited", it) }
        styles.firstChild()?.let { outOneCssRule(out, className, ":first-child", it) }
        styles.lastChild()?.let { outOneCssRule(out, className, ":last-child", it) }
        styles.checked()?.let { outOneCssRule(out, className, ":checked", it) }
        styles.disabled()?.let { outOneCssRule(out, className, ":disabled", it) }
        styles.enabled()?.let { outOneCssRule(out, className, ":enabled", it) }
        styles.required()?.let { outOneCssRule(out, className, ":required", it) }
        styles.optional()?.let { outOneCssRule(out, className, ":optional", it) }
        styles.empty()?.let { outOneCssRule(out, className, ":empty", it) }
        styles.firstOfType()?.let { outOneCssRule(out, className, ":first-of-type", it) }
        styles.lastOfType()?.let { outOneCssRule(out, className, ":last-of-type", it) }
        styles.onlyChild()?.let { outOneCssRule(out, className, ":only-child", it) }
        styles.onlyOfType()?.let { outOneCssRule(out, className, ":only-of-type", it) }
        styles.target()?.let { outOneCssRule(out, className, ":target", it) }

        styles.before()?.let { outOneCssRule(out, className, "::before", it) }
        styles.after()?.let { outOneCssRule(out, className, "::after", it) }
        styles.firstLine()?.let { outOneCssRule(out, className, "::first-line", it) }
        styles.firstLetter()?.let { outOneCssRule(out, className, "::first-letter", it) }
        styles.selection()?.let { outOneCssRule(out, className, "::selection", it) }
        styles.placeholder()?.let { outOneCssRule(out, className, "::placeholder", it) }
        styles.marker()?.let { outOneCssRule(out, className, "::marker", it) }
        styles.fileSelectorButton()?.let { outOneCssRule(out, className, "::file-selector-button", it) }

        styles.additionals()?.forEach { (pseudo, styles) ->
            outOneCssRule(out, className, pseudo, styles)
        }
    }

    private fun outOneCssRule(out: Appendable, className: String, addition: String, styles: String) {
        out.append('.').append(className).append(addition).append(" {")
            .append(styles.replace('\r', ' ').replace('\n', ' '))
            .append("}\n")
    }

    companion object {

        fun <T : CssClass> getClassName(css: KClass<T>): String? =
            instance.classes[css]?.cssClassName()

        lateinit var instance: CssHelper
    }
}