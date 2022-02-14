package no.nav.aap.api.oppslag.fastlege

import no.nav.aap.api.oppslag.fastlege.FastlegeConfig.Companion.FASTLEGE
import no.nav.aap.rest.AbstractWebClientAdapter
import no.nav.aap.util.AuthContext
import no.nav.aap.util.LoggerUtil
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono


@Component
class FastlegeClientAdapter(
        @Qualifier(FASTLEGE) webClient: WebClient,
        private val cf: FastlegeConfig,
        private val authContext: AuthContext) : AbstractWebClientAdapter(webClient, cf) {

    private val log = LoggerUtil.getLogger(javaClass)
    fun fastlege() = authContext.getSubject()?.let {
        webClient
            .get()
            .uri { b -> b.path(cf.path).build() }
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus({ obj: HttpStatus -> obj.isError }) { obj: ClientResponse -> obj.createException() }
            .bodyToMono<FastlegeDTO>()
            .map { this::tilFastlege }
            .block()
    }

    private fun tilFastlege(dto: FastlegeDTO) = Fastlege() // TODO

    override fun toString() = "${javaClass.simpleName} [webClient=$webClient,authContext=$authContext, cfg=$cf]"
}

class Fastlege {

}