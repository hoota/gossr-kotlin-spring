package kiss.gossr.spring

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = ["kiss.gossr.spring"])
open class TestApplicationConfig {

    @Bean
    open fun mockMvc(webApplicationContext: WebApplicationContext): MockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

    @Bean
    open fun routesHelper(
        handlerMapping: RequestMappingHandlerMapping,
        applicationContext: ApplicationContext
    ) = RoutesHelper(
        applicationContext, handlerMapping
    )
}

@Component
@RouteHandler
class TestRouteHandler {
    data class SimpleRoute(
        @PathProperty
        val a: Int,
        val b: String,
        val c: UUID,
        val d: Double? = null
    ) : GetRoute

    @RouteHandler
    fun simpleRouteHandler(route: SimpleRoute): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                DIV { + route.a.toString() }
                DIV { + route.b }
                DIV { + route.c }
            }
        }
    }

    data class SimplePostRoute(
        val a: Int,
        val b: Double
    ) : PostRoute

    @RouteHandler
    fun simplePostRouteHandler(route: SimplePostRoute): View {
        return object : GossSpringRenderer(), GossrSpringView {
            override fun draw() {
                DIV { + route.a.toString() }
                DIV { + formatMoney2(route.b) }
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
        val route = TestRouteHandler.SimpleRoute(a = 123, b = "Hello", c = UUID.randomUUID())
        val url = RoutesHelper.getRouteUrl(route)

        assertEquals("/simple/123?b=Hello&c=${route.c}", url)

        mockMvc.perform(MockMvcRequestBuilders.get(url))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<DIV>123</DIV>\n" +
                "<DIV>Hello</DIV>\n" +
                "<DIV>${route.c}</DIV>\n"))
    }

    @Test
    fun testPost() {
        val route = TestRouteHandler.SimplePostRoute(a = 123, b = 1234.5678)
        val url = RoutesHelper.getRouteUrlPath(route)

        assertEquals("/simple/post", url)

        mockMvc.perform(
            MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("a", route.a.toString())
                .param("b", route.b.toString())
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("<DIV>123</DIV>\n" +
                "<DIV>1234.57</DIV>\n"))
    }

}