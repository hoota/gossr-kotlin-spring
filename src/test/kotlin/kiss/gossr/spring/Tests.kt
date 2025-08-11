package kiss.gossr.spring

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.View
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.LocalDate
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.test.Test
import kotlin.test.assertEquals

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = ["kiss.gossr.spring"])
open class TestApplicationConfig : WebMvcConfigurer {

    @Bean
    open fun mockMvc(webApplicationContext: WebApplicationContext): MockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(LocalDateRequestParamConverter())
    }
}

@Component
@RouteHandler
class TestRouteHandler {
    data class SimpleRoute(
        @PathProperty
        val a: Int,
        val b: String,
        val c: UUID,
        val d: Double? = null,
        val e: List<Int>
    ) : GetRoute

    @RouteHandler
    fun getRouteHandler(route: SimpleRoute): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                DIV { + route.a.toString() }
                DIV { + route.b }
                DIV { + route.c }
                DIV { + route.e.joinToString("-")}
                SPAN { +"GET" }
            }
        }
    }

    data class SimplePostRoute(
        val a: Int,
        val b: Double,
        val c: LocalDate?
    ) : PostRoute

    @RouteHandler
    fun postRouteHandler(route: SimplePostRoute): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                DIV { + route.a.toString() }
                DIV { + formatMoney2(route.b) }
                DIV { + formatDate(route.c) }
                SPAN { +"POST" }
            }
        }
    }

    class SimplePutRoute : PutRoute

    @RouteHandler
    fun putRouteHandler(route: SimplePutRoute): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                SPAN { +"PUT" }
            }
        }
    }

    class SimpleDeleteRoute : DeleteRoute

    @RouteHandler
    fun delRouteHandler(route: SimpleDeleteRoute): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                SPAN { +"DELETE" }
            }
        }
    }

    class SimpleMultiRoute : GetRoute, PostRoute

    @RouteHandler
    fun multiRouteHandler(
        request: HttpServletRequest,
        route: SimpleMultiRoute
    ): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                SPAN { +request.method }
            }
        }
    }

}

@RunWith(SpringJUnit4ClassRunner::class)
@WebAppConfiguration
@ContextConfiguration(classes = [TestApplicationConfig::class])
class Tests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun testGet() {
        val route = TestRouteHandler.SimpleRoute(a = 123, b = "Hello", c = UUID.randomUUID(), e = listOf(1,2,3))
        val url = RoutesHelper.getRouteUrl(route)

        assertEquals("/simple/123?b=Hello&c=${route.c}&e=1&e=2&e=3", url)

        mockMvc.perform(MockMvcRequestBuilders.get(url))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<DIV>123</DIV>\n" +
                "<DIV>Hello</DIV>\n" +
                "<DIV>${route.c}</DIV>\n" +
                "<DIV>1-2-3</DIV>\n" +
                "<SPAN>GET</SPAN>\n")
            )
    }

    @Test
    fun testPost() {
        val today = LocalDate.now()
        val route = TestRouteHandler.SimplePostRoute(a = 123, b = 1234.5678, c = today)
        val url = RoutesHelper.getRouteUrlPath(route)

        assertEquals("/simple/post", url)

        mockMvc.perform(
            MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("a", route.a.toString())
                .param("b", route.b.toString())
                .param("c", route.c.toString())
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<DIV>123</DIV>\n" +
                "<DIV>1234.57</DIV>\n"+
                "<DIV>$today</DIV>\n"+
                "<SPAN>POST</SPAN>\n")
            )
    }

    @Test
    fun testPut() {
        val route = TestRouteHandler.SimplePutRoute()
        val url = RoutesHelper.getRouteUrlPath(route)

        assertEquals("/simple/put", url)

        mockMvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<SPAN>PUT</SPAN>\n"))
    }

    @Test
    fun testDel() {
        val route = TestRouteHandler.SimpleDeleteRoute()
        val url = RoutesHelper.getRouteUrlPath(route)

        assertEquals("/simple/delete", url)

        mockMvc.perform(
            MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<SPAN>DELETE</SPAN>\n"))
    }

    @Test
    fun testMulti() {
        val route = TestRouteHandler.SimpleMultiRoute()
        val url = RoutesHelper.getRouteUrlPath(route)

        assertEquals("/simple/multi", url)

        mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<SPAN>GET</SPAN>\n"))

        mockMvc.perform(
            MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<SPAN>POST</SPAN>\n"))

        mockMvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed)

        mockMvc.perform(
            MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed)
    }
}