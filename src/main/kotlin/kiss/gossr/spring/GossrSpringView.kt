package kiss.gossr.spring

import kiss.gossr.GossRenderer
import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface GossrSpringView : View {
    override fun render(
        params: MutableMap<String, *>?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {

        response.also {
            it.contentType = "text/html"
        }.outputStream.bufferedWriter().use { out ->
            GossRenderer.use(
                out = out,
                dateTimeFormats = GossSpringRenderer.getDateTimeFormats(),
                moneyFormats = GossSpringRenderer.getMoneyFormats(),
                renderFunction = this::draw
            )
        }
    }

    fun draw()
}