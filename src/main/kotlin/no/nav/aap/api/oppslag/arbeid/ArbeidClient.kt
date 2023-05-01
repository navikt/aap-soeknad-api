package no.nav.aap.api.oppslag.arbeid

import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Component
import no.nav.aap.api.oppslag.arbeid.ArbeidConfig.Companion.ARBEID

@Component
@Observed(contextualName = ARBEID)
class ArbeidClient(private val arbeid : ArbeidWebClientAdapter,
                   private val org : OrganisasjonWebClientAdapter) {

    fun arbeidInfo() = arbeid.arbeidInfo().map { it.tilArbeidInfo(org.orgNavn(it.arbeidsgiver.organisasjonsnummer)) }
}