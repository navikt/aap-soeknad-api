package no.nav.aap.api.oppslag.behandler

import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Component
import no.nav.aap.api.oppslag.behandler.BehandlerConfig.Companion.BEHANDLER

@Component
@Observed(contextualName = BEHANDLER)
class BehandlerClient(private val adapter : BehandlerWebClientAdapter) {

    fun behandlerInfo() = adapter.behandlerInfo()
}