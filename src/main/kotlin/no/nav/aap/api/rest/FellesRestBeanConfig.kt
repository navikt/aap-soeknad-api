package no.nav.aap.api.rest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import no.nav.aap.api.rest.tokenx.TokenXModule
import no.nav.boot.conditionals.ConditionalOnDevOrLocal
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.trace.http.HttpExchangeTracer
import org.springframework.boot.actuate.trace.http.HttpTraceRepository
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.boot.actuate.web.trace.servlet.HttpTraceFilter
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.zalando.problem.jackson.ProblemModule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest


@Configuration
class FellesRestBeanConfig {
    @Bean
    fun customizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { b: Jackson2ObjectMapperBuilder ->
            b.modules(ProblemModule(), JavaTimeModule(), TokenXModule(), KotlinModule())
        }
    }
    @Bean
    @ConditionalOnDevOrLocal
    fun httpTraceRepository(): HttpTraceRepository {
        return InMemoryHttpTraceRepository()
    }
    @Bean
    fun swagger(p: BuildProperties): OpenAPI? {
        return OpenAPI()
            .info(
                Info().title("AAP søknadmottaker")
                    .description("Mottak av søknader")
                    .version(p.version)
                    .license(License().name("MIT").url("http://nav.no"))
            )
    }

    @ConditionalOnDevOrLocal
    class ActuatorIgnoringTraceRequestFilter(repository: HttpTraceRepository?, tracer: HttpExchangeTracer?) :
        HttpTraceFilter(repository, tracer) {
        @Throws(ServletException::class)
        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            return request.servletPath.contains("actuator") || request.servletPath.contains("swagger")
        }
    }
    @Component
    class StartupInfoContributor(val ctx: ApplicationContext) : InfoContributor {
        override fun contribute(builder: org.springframework.boot.actuate.info.Info.Builder) {
            builder.withDetail("extra-info", mapOf(
                "Startup time" to LocalDateTime.ofInstant(Instant.ofEpochMilli(ctx.startupDate), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ))
        }
    }

    @ConditionalOnDevOrLocal
    fun requestLoggingFilter(): CommonsRequestLoggingFilter? {
        val loggingFilter = CommonsRequestLoggingFilter()
        loggingFilter.setIncludeClientInfo(true)
        loggingFilter.setIncludeQueryString(true)
        loggingFilter.setIncludePayload(true)
        loggingFilter.setIncludeHeaders(false)
        return loggingFilter
    }

}