package no.nav.aap.api.søknad.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.ObjectMapper
import com.neovisionaries.i18n.CountryCode
import no.nav.aap.api.felles.Fødselsnummer
import no.nav.aap.api.felles.Periode
import no.nav.aap.api.oppslag.behandler.Behandler
import no.nav.aap.api.søknad.model.AnnetBarnOgInntekt.Relasjon.FORELDER
import no.nav.aap.api.søknad.model.Søker.Barn
import no.nav.aap.api.søknad.model.Utbetaling.AnnenStønadstype.AFP
import no.nav.aap.joark.DokumentVariant
import no.nav.aap.joark.Filtype.JSON
import no.nav.aap.joark.VariantFormat.ORIGINAL
import no.nav.aap.util.StringExtensions.toEncodedJson
import java.time.LocalDate
import java.util.*

data class StandardSøknad(
        val studier: Studier,
        val startdato: Startdato,
        val ferie: Ferie,
        val medlemsskap: Medlemskap,
        val behandlere: List<Behandler>,
        val yrkesskadeType: RadioValg,
        val utbetalinger: Utbetaling?,
        val registrerteBarn: List<BarnOgInntekt> = emptyList(),
        val andreBarn: List<AnnetBarnOgInntekt> = emptyList(),
        val tilleggsopplysninger: String?,
        override val vedlegg: UUID? = null) : VedleggAware {

    fun asJsonVariant(mapper: ObjectMapper) = DokumentVariant(JSON, this.toEncodedJson(mapper), ORIGINAL)
}

interface VedleggAware {
    val vedlegg: UUID?
}

data class Studier(@JsonAlias("erStudent") val svar: StudieSvar?,
                   val kommeTilbake: RadioValg?,
                   override val vedlegg: UUID? = null) : VedleggAware {
    enum class StudieSvar {
        JA,
        NEI,
        AVBRUTT
    }

}

data class Startdato(val fom: LocalDate, val hvorfor: Hvorfor?, val beskrivelse: String?) {
    enum class Hvorfor {
        HELSE,
        FEILINFO
    }
}

data class Medlemskap(val boddINorgeSammenhengendeSiste5: Boolean,
                      val jobbetUtenforNorgeFørSyk: Boolean?,
                      val jobbetSammenhengendeINorgeSiste5: Boolean?,
                      val iTilleggArbeidUtenforNorge: Boolean?,
                      val utenlandsopphold: List<Utenlandsopphold>)

data class Utenlandsopphold(val land: CountryCode,
                            val periode: Periode,
                            val arbeidet: Boolean,
                            val id: String?) {

    val landnavn = land.toLocale().displayCountry
}

data class Ferie(val ferieType: FerieType, val periode: Periode? = null, val dager: Int? = null) {
    enum class FerieType {
        PERIODE,
        DAGER,
        NEI,
        VET_IKKE
    }
}

data class BarnOgInntekt(val fnr: Fødselsnummer, val merEnnIG: Boolean? = false, val barnepensjon: Boolean = false)
data class AnnetBarnOgInntekt(val barn: Barn,
                              val relasjon: Relasjon = FORELDER,
                              val merEnnIG: Boolean? = false,
                              val barnepensjon: Boolean = false) {
    enum class Relasjon {
        FOSTERFORELDER,
        FORELDER
    }
}

enum class RadioValg {
    JA,
    NEI,
    VET_IKKE
}

data class Utbetaling(val fraArbeidsgiver: Boolean,
                      @JsonAlias("stønadstyper") val andreStønader: List<AnnenStønad> = emptyList(),
                      val ekstraUtbetaling: EkstraUtbetaling? = null) {
    data class EkstraUtbetaling(val hvilken: String, val hvem: String, override val vedlegg: UUID? = null) :
        VedleggAware

    data class AnnenStønad(val type: AnnenStønadstype,
                           val hvemUtbetalerAFP: String? = null,
                           override val vedlegg: UUID? = null) : VedleggAware {
        init {
            if (type != AFP && hvemUtbetalerAFP != null) {
                throw IllegalStateException("Hvem utbetaler kun for AFP")
            }
        }
    }

    enum class AnnenStønadstype {
        KVALIFISERINGSSTØNAD,
        ØKONOMISK_SOSIALHJELP,
        INTRODUKSJONSSTØNAD,
        OMSORGSSTØNAD,
        STIPEND,
        AFP,
        VERV,
        UTLAND,
        ANNET,
        NEI
    }
}