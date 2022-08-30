package no.nav.aap.api.oppslag

import no.nav.aap.api.oppslag.arbeid.ArbeidClient
import no.nav.aap.api.oppslag.behandler.BehandlerClient
import no.nav.aap.api.oppslag.konto.KontoClient
import no.nav.aap.api.oppslag.krr.KRRClient
import no.nav.aap.api.oppslag.pdl.PDLClient
import no.nav.aap.api.oppslag.saf.SafClient
import no.nav.aap.api.oppslag.søknad.SøknadClient
import no.nav.aap.api.søknad.model.SøkerInfo
import no.nav.aap.joark.DokumentInfoId
import no.nav.aap.util.Constants
import no.nav.aap.util.LoggerUtil.getLogger
import no.nav.security.token.support.spring.ProtectedRestController
import org.springframework.http.CacheControl.noCache
import org.springframework.http.ContentDisposition.attachment
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity.notFound
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@ProtectedRestController(value = ["/oppslag"], issuer = Constants.IDPORTEN)
class OppslagController(val pdl: PDLClient,
                        val behandler: BehandlerClient,
                        val arbeid: ArbeidClient,
                        val krr: KRRClient,
                        val søknad: SøknadClient,
                        val konto: KontoClient,
                        val saf: SafClient) {

    val log = getLogger(javaClass)

    @GetMapping("/soeker")
    fun søker() = SøkerInfo(
            pdl.søkerMedBarn(),
            behandler.behandlere(),
            arbeid.arbeidsforhold(),
            krr.kontaktinfo())
        .also {
            log.trace("Søker er $it")
            log.trace("Konto ${konto.kontoinfo()}")
        }

    @GetMapping("/soeknader")
    fun søknader(@RequestParam fra: LocalDate = LocalDate.now().minusYears(3)) = søknad.søknader(fra)

    @GetMapping("/saf")
    fun dokument(@PathVariable journalpostId: String, @PathVariable dokumentInfoId: DokumentInfoId) =
        saf.dokument(journalpostId, dokumentInfoId)
            ?.let {
                ok()
                    .cacheControl(noCache().mustRevalidate())
                    .headers(HttpHeaders().apply {
                        contentDisposition = attachment()
                            .build()
                    })
                    .body(it)
            } ?: notFound().build()
}