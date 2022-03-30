package no.nav.aap.api.oppslag.behandler

import org.springframework.stereotype.Component

@Component
class BehandlerClient(private val adapter: BehandlerWebClientAdapter) {
    fun behandlere() = adapter.behandlere()
    fun behandlereM() = adapter.behandlereM()

}