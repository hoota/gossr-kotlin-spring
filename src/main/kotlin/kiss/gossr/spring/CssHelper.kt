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
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

open class CssStyles(
    val setupStype: (CssStyles.() -> Unit)? = null
) {
    var style: String? = null
    var hover: String? = null
    var active: String? = null
    var focus: String? = null
    var visited: String? = null
    var firstChild: String? = null
    var lastChild: String? = null
    var checked: String? = null
    var disabled: String? = null
    var enabled: String? = null
    var required: String? = null
    var optional: String? = null
    var empty: String? = null
    var firstOfType: String? = null
    var lastOfType: String? = null
    var onlyChild: String? = null
    var onlyOfType: String? = null
    var target: String? = null

    var before: String? = null
    var after: String? = null
    var firstLine: String? = null
    var firstLetter: String? = null
    var selection: String? = null
    var placeholder: String? = null
    var marker: String? = null
    var fileSelectorButton: String? = null

    var additional: MutableMap<String, String>? = null

    fun add(selector: String, value: ()->String) {
        if(additional == null) additional = HashMap()
        additional?.put(selector, value())
    }

    init {
        setupStype?.invoke(this)
    }
}

open class CssClass(
    val classSetup: (CssClass.() -> Unit)? = null
) : CssStyles() {
    var cssClassName: String

    var medias: MutableMap<String, CssStyles>? = null

    init {
        cssClassName = getCssClassName(this.javaClass)
        classSetup?.invoke(this)
        map[this.javaClass] = cssClassName
    }

    fun media(selector: String, styleSetup: CssStyles.() -> Unit) {
        if(medias == null) medias = HashMap()
        medias?.put(selector, CssStyles(styleSetup))
    }

    companion object {
        private val id = AtomicInteger()
        private val map = HashMap<Class<out CssClass>, String>()

        fun <T : CssClass> getCssClassName(javaClass: Class<T>): String {
            return map.computeIfAbsent(javaClass) {
                id.getAndIncrement().let { "gossr-$it" }
            }
        }
    }
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
                outOneCssClass(out, it.cssClassName, it)
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

        classes = applicationContext.getBeansOfType(CssClass::class.java)
            .values
            .associateBy { it.javaClass.kotlin }

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

            classes.values.forEach { cls ->
                cls.classSetup?.invoke(cls)
                outOneCssClass(out, cls.cssClassName, cls)
                cls.medias?.forEach { (media, style) ->
                    style.setupStype?.invoke(style)
                    medias.computeIfAbsent(media) { ArrayList() }.add(cls.cssClassName to style)
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
        styles.style?.let { outOneCssRule(out, className, "", it) }
        styles.hover?.let { outOneCssRule(out, className, ":hover", it) }
        styles.active?.let { outOneCssRule(out, className, ":active", it) }
        styles.focus?.let { outOneCssRule(out, className, ":focus", it) }
        styles.visited?.let { outOneCssRule(out, className, ":visited", it) }
        styles.firstChild?.let { outOneCssRule(out, className, ":first-child", it) }
        styles.lastChild?.let { outOneCssRule(out, className, ":last-child", it) }
        styles.checked?.let { outOneCssRule(out, className, ":checked", it) }
        styles.disabled?.let { outOneCssRule(out, className, ":disabled", it) }
        styles.enabled?.let { outOneCssRule(out, className, ":enabled", it) }
        styles.required?.let { outOneCssRule(out, className, ":required", it) }
        styles.optional?.let { outOneCssRule(out, className, ":optional", it) }
        styles.empty?.let { outOneCssRule(out, className, ":empty", it) }
        styles.firstOfType?.let { outOneCssRule(out, className, ":first-of-type", it) }
        styles.lastOfType?.let { outOneCssRule(out, className, ":last-of-type", it) }
        styles.onlyChild?.let { outOneCssRule(out, className, ":only-child", it) }
        styles.onlyOfType?.let { outOneCssRule(out, className, ":only-of-type", it) }
        styles.target?.let { outOneCssRule(out, className, ":target", it) }

        styles.before?.let { outOneCssRule(out, className, "::before", it) }
        styles.after?.let { outOneCssRule(out, className, "::after", it) }
        styles.firstLine?.let { outOneCssRule(out, className, "::first-line", it) }
        styles.firstLetter?.let { outOneCssRule(out, className, "::first-letter", it) }
        styles.selection?.let { outOneCssRule(out, className, "::selection", it) }
        styles.placeholder?.let { outOneCssRule(out, className, "::placeholder", it) }
        styles.marker?.let { outOneCssRule(out, className, "::marker", it) }
        styles.fileSelectorButton?.let { outOneCssRule(out, className, "::file-selector-button", it) }

        styles.additional?.forEach { (pseudo, styles) ->
            outOneCssRule(out, className, pseudo, styles)
        }
    }

    private fun outOneCssRule(out: Appendable, className: String, addition: String, styles: String) {
        out.append('.').append(className).append(addition).append(" {")
            .append(styles.replace('\r', ' ').replace('\n', ' '))
            .append("}\n")
    }

    companion object {
        lateinit var instance: CssHelper
    }
}