package kiss.gossr.spring

import jakarta.servlet.http.HttpServletResponse
import org.intellij.lang.annotations.Language
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

open class CssStyles(
    val setupStype: (CssStyles.() -> Unit)? = null
) {
    @Language("css", prefix = "{", suffix = "}")
    var style: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var hover: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var active: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var focus: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var visited: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var firstChild: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var lastChild: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var checked: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var disabled: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var enabled: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var required: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var optional: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var empty: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var firstOfType: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var lastOfType: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var onlyChild: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var onlyOfType: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var target: String? = null

    @Language("css", prefix = "{", suffix = "}")
    var before: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var after: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var firstLine: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var firstLetter: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var selection: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var placeholder: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var marker: String? = null
    @Language("css", prefix = "{", suffix = "}")
    var fileSelectorButton: String? = null

    var additional: MutableMap<String, String>? = null

    fun add(selector: String, @Language("css", prefix = "{", suffix = "}") value: String) {
        if(additional == null) additional = HashMap()
        additional?.put(selector, value)
    }

    init {
        updateStyle()
    }

    fun updateStyle() {
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
        updateClass()
    }

    fun media(
        @Language("css", prefix = "media(", suffix = "){}")
        selector: String,
        styleSetup: CssStyles.() -> Unit
    ) {
        if(medias == null) medias = HashMap()
        medias?.put(selector, CssStyles(styleSetup))
    }

    fun updateClass() {
        classSetup?.invoke(this)
        map[this.javaClass] = cssClassName
    }

    companion object {
        private val id = AtomicInteger()
        private val map = ConcurrentHashMap<Class<out CssClass>, String>()

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

            outAll(out)

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
            outAll(out)
        }
    }

    private fun outAll(out: java.lang.Appendable) {
        val medias = HashMap<String, ArrayList<Pair<String, CssStyles>>>()

        classes.values.forEach { cls ->
            appendOneCssClass(out, medias, cls)
        }

        medias.forEach { (media, styles) ->
            appendMedia(out, media, styles)
        }
    }

    private fun appendMedia(out: java.lang.Appendable, media: String, styles: java.util.ArrayList<Pair<String, CssStyles>>) {
        out.append("@media(").append(media).append(") {\n")
        styles.forEach { appendOneCssStyle(out, it.first, it.second) }
        out.append("}\n")
    }

    private fun appendOneCssClass(
        out: Appendable,
        medias: HashMap<String, ArrayList<Pair<String, CssStyles>>>,
        cls: CssClass
    ) {
        if(devMode) {
            // updating css-class data to support JVM hot reload in debug mode
            cls.updateClass()
        }

        appendOneCssStyle(out, cls.cssClassName, cls)

        cls.medias?.forEach { (media, style) ->
            if(devMode) {
                // updating css-class data to support JVM hot reload in debug mode
                style.updateStyle()
            }
            medias.computeIfAbsent(media) { ArrayList() }.add(cls.cssClassName to style)
        }
    }

    private fun appendOneCssStyle(out: Appendable, className: String, styles: CssStyles) {
        styles.style?.let { appendOneCssRule(out, className, "", it) }
        styles.hover?.let { appendOneCssRule(out, className, ":hover", it) }
        styles.active?.let { appendOneCssRule(out, className, ":active", it) }
        styles.focus?.let { appendOneCssRule(out, className, ":focus", it) }
        styles.visited?.let { appendOneCssRule(out, className, ":visited", it) }
        styles.firstChild?.let { appendOneCssRule(out, className, ":first-child", it) }
        styles.lastChild?.let { appendOneCssRule(out, className, ":last-child", it) }
        styles.checked?.let { appendOneCssRule(out, className, ":checked", it) }
        styles.disabled?.let { appendOneCssRule(out, className, ":disabled", it) }
        styles.enabled?.let { appendOneCssRule(out, className, ":enabled", it) }
        styles.required?.let { appendOneCssRule(out, className, ":required", it) }
        styles.optional?.let { appendOneCssRule(out, className, ":optional", it) }
        styles.empty?.let { appendOneCssRule(out, className, ":empty", it) }
        styles.firstOfType?.let { appendOneCssRule(out, className, ":first-of-type", it) }
        styles.lastOfType?.let { appendOneCssRule(out, className, ":last-of-type", it) }
        styles.onlyChild?.let { appendOneCssRule(out, className, ":only-child", it) }
        styles.onlyOfType?.let { appendOneCssRule(out, className, ":only-of-type", it) }
        styles.target?.let { appendOneCssRule(out, className, ":target", it) }

        styles.before?.let { appendOneCssRule(out, className, "::before", it) }
        styles.after?.let { appendOneCssRule(out, className, "::after", it) }
        styles.firstLine?.let { appendOneCssRule(out, className, "::first-line", it) }
        styles.firstLetter?.let { appendOneCssRule(out, className, "::first-letter", it) }
        styles.selection?.let { appendOneCssRule(out, className, "::selection", it) }
        styles.placeholder?.let { appendOneCssRule(out, className, "::placeholder", it) }
        styles.marker?.let { appendOneCssRule(out, className, "::marker", it) }
        styles.fileSelectorButton?.let { appendOneCssRule(out, className, "::file-selector-button", it) }

        styles.additional?.forEach { (pseudo, styles) ->
            appendOneCssRule(out, className, pseudo, styles)
        }
    }

    private fun appendOneCssRule(out: Appendable, className: String, addition: String, styles: String) {
        out.append('.').append(className).append(addition).append(" {")
            .append(styles.replace('\r', ' ').replace('\n', ' '))
            .append("}\n")
    }

    companion object {
        lateinit var instance: CssHelper
    }
}