package no.nav.aap.api.søknad.tokenx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.net.URI

class TokenXConfig {
    @Bean
    fun configMatcher(): TokenXConfigMatcher {
        return object : TokenXConfigMatcher {
            override fun findProperties(configs: ClientConfigurationProperties, request: URI): ClientProperties? {
                return configs.registration[request.host.split("\\.".toRegex()).toTypedArray()[0]]
            }
        }
    }
    @Bean
    fun customizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { b: Jackson2ObjectMapperBuilder ->
            b.mixIn(
                OAuth2AccessTokenResponse::class.java, IgnoreUnknownMixin::class.java
            )
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private interface IgnoreUnknownMixin
}