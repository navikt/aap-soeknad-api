package no.nav.aap.api.søknad.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.neovisionaries.i18n.CountryCode
import no.nav.aap.api.felles.Periode
import no.nav.aap.api.oppslag.behandler.AnnenBehandler
import no.nav.aap.api.oppslag.behandler.RegistrertBehandler
import no.nav.aap.api.søknad.model.AnnetBarnOgInntekt.Relasjon.FORELDER
import no.nav.aap.api.søknad.model.Studier.StudieSvar.AVBRUTT
import no.nav.aap.api.søknad.model.Søker.Barn
import no.nav.aap.api.søknad.model.Utbetalinger.AnnenStønadstype.AFP
import no.nav.aap.api.søknad.model.Utbetalinger.AnnenStønadstype.OMSORGSSTØNAD
import no.nav.aap.api.søknad.model.Utbetalinger.AnnenStønadstype.UTLAND
import no.nav.aap.api.søknad.model.VedleggType.ANDREBARN
import no.nav.aap.api.søknad.model.VedleggType.ANNET
import no.nav.aap.api.søknad.model.VedleggType.ARBEIDSGIVER
import no.nav.aap.api.søknad.model.VedleggType.OMSORG
import no.nav.aap.api.søknad.model.VedleggType.STUDIER
import no.nav.aap.joark.DokumentVariant
import no.nav.aap.joark.Filtype.JSON
import no.nav.aap.joark.VariantFormat.ORIGINAL
import no.nav.aap.util.LoggerUtil.getLogger
import no.nav.aap.util.StringExtensions.toEncodedJson
import java.io.IOException
import java.util.*

data class StandardSøknad(
        val studier: Studier,
        val startdato: Startdato,
        val ferie: Ferie,
        val medlemsskap: Medlemskap,
        @JsonAlias("behandlere")
        val registrerteBehandlere: List<RegistrertBehandler> = emptyList(),
        @JsonAlias("manuelleBehandlere")
        val andreBehandlere: List<AnnenBehandler> = emptyList(),
        val yrkesskadeType: RadioValg,
        val utbetalinger: Utbetalinger?,
        val registrerteBarn: List<BarnOgInntekt> = emptyList(),
        val andreBarn: List<AnnetBarnOgInntekt> = emptyList(),
        val tilleggsopplysninger: String?,
        @JsonAlias("andreVedlegg") override val vedlegg: Vedlegg? = null) : VedleggAware {

    private val log = getLogger(javaClass)

    fun somJsonVariant(mapper: ObjectMapper) = DokumentVariant(JSON, toEncodedJson(mapper), ORIGINAL)

    fun innsendteVedlegg(): List<VedleggType> {

    }

    fun vedlegg(): Pair<MutableList<VedleggType>, MutableList<VedleggType>> {

        val mangler = mutableListOf<VedleggType>()
        val innsendte = mutableListOf<VedleggType>()

        log.trace("Sjekker vedlegg studier $studier")
        if (studier.erStudent == AVBRUTT && manglerVedlegg(studier)) {
            log.trace("Fant mangler for ${STUDIER.tittel}").also {
                mangler += STUDIER
            }
        }
        else {
            if (harVedlegg(studier)) {
                log.trace("Studier har vedlegg")
                innsendte += STUDIER
            }
            log.trace("Ingen mangler for studier")
        }
        with(andreBarn) {
            log.trace("Sjekker vedlegg andre barn $andreBarn")
            if (count() > count { (it.vedlegg?.deler?.size ?: 0) > 0 }) {
                mangler += ANDREBARN.also {
                    log.trace("Fant mangler for ${ANDREBARN.tittel}")
                }
            }
            else {
                if (harVedlegg(this)) {
                    log.trace("barn har vedlegg")
                    innsendte += ANDREBARN
                }
                log.trace("Ingen mangler for andre barn")
            }
        }
        with(utbetalinger) {
            log.trace("Sjekker vedlegg arbeidsgiver ${this?.ekstraFraArbeidsgiver}")
            if (this?.ekstraFraArbeidsgiver?.fraArbeidsgiver == true && manglerVedlegg(ekstraFraArbeidsgiver)) {
                mangler += ARBEIDSGIVER.also {
                    log.trace("Fant mangler for ${ARBEIDSGIVER.tittel}")
                }
            }
            else {
                log.trace("Ingen mangler for arbeidsgiver")
            }
            log.trace("Sjekker vedlegg andre stønader ${this?.andreStønader}")
            this?.andreStønader?.firstOrNull() { it.type == OMSORGSSTØNAD }?.let {
                if (manglerVedlegg(it)) {
                    mangler += OMSORG.also {
                        log.trace("Fant mangler for ${OMSORG.tittel}")
                    }
                }
                else {
                    if (harVedlegg(it)) {
                        log.trace("omsorg har vedlegg")
                        innsendte += OMSORG
                    }
                    log.trace("Ingen mangler for omsorg")
                }
            }
            this?.andreStønader?.firstOrNull() { it.type == UTLAND }?.let {
                if (manglerVedlegg(it)) {
                    mangler += VedleggType.UTLAND.also {
                        log.trace("Fant mangler for ${VedleggType.UTLAND.tittel}")
                    }
                }
                else {
                    if (harVedlegg(it)) {
                        log.trace("utland har vedlegg")
                        innsendte += VedleggType.UTLAND
                    }
                    log.trace("Ingen mangler for utland")
                }
            }
        }
        if (vedlegg?.deler?.isNotEmpty() == true) {
            log.trace("Vi har andre vedlegg")
            innsendte += ANNET
        }

        return Pair(innsendte.also {
            if (it.isEmpty()) {
                log.trace("Vi har ingen vedlegg")
            }
            else {
                log.trace("Vi har  følgende vedlegg $it")
            }
        }, mangler.also {
            if (it.isEmpty()) {
                log.trace("Det mangler ingen vedlegg")
            }
            else {
                log.trace("Det mangler følgende vedlegg $it")
            }
        })
    }

}

fun manglerVedlegg(v: VedleggAware) = v.vedlegg?.deler?.isEmpty() == true
fun harVedlegg(v: VedleggAware) = v.vedlegg?.deler?.isNotEmpty() == true

private fun harVedlegg(v: List<VedleggAware>) = v.find { harVedlegg(it) } != null

@JsonDeserialize(using = VedleggDeserializer::class)
data class Vedlegg(val tittel: String? = null, @JsonValue val deler: List<UUID?>? = null)

interface VedleggAware {
    val vedlegg: Vedlegg?
}

data class Studier(val erStudent: StudieSvar?,
                   val kommeTilbake: RadioValg?,
                   override val vedlegg: Vedlegg? = null) : VedleggAware {

    enum class StudieSvar {
        JA,
        NEI,
        AVBRUTT
    }

}

data class Startdato(val beskrivelse: String?)

data class Medlemskap(val boddINorgeSammenhengendeSiste5: Boolean,
                      val jobbetUtenforNorgeFørSyk: Boolean?,
                      val jobbetSammenhengendeINorgeSiste5: Boolean?,
                      val iTilleggArbeidUtenforNorge: Boolean?,
                      val utenlandsopphold: List<Utenlandsopphold> = emptyList())

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

data class BarnOgInntekt(val merEnnIG: Boolean? = false, val barnepensjon: Boolean = false)
data class AnnetBarnOgInntekt(val barn: Barn,
                              val relasjon: Relasjon = FORELDER,
                              val merEnnIG: Boolean? = false,
                              val barnepensjon: Boolean = false,
                              override val vedlegg: Vedlegg? = null) : VedleggAware {

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

data class Utbetalinger(val ekstraFraArbeidsgiver: FraArbeidsgiver,
                        @JsonAlias("stønadstyper") val andreStønader: List<AnnenStønad> = emptyList()) {

    data class FraArbeidsgiver(val fraArbeidsgiver: Boolean,
                               override val vedlegg: Vedlegg? = null) : VedleggAware

    data class AnnenStønad(val type: AnnenStønadstype,
                           val hvemUtbetalerAFP: String? = null,
                           override val vedlegg: Vedlegg? = null) : VedleggAware {

        init {
            require((type == AFP && hvemUtbetalerAFP != null) || (type != AFP && hvemUtbetalerAFP == null))
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

enum class VedleggType(val tittel: String) {
    ARBEIDSGIVER("Dokumentasjon av ekstra utbetaling fra arbeidsgiver"),
    STUDIER("Dokumentasjon av studier"),
    ANDREBARN("Dokumentasjon av andre barn"),
    OMSORG("Dokumentasjon av omsorgsstønad fra kommunen"),
    UTLAND("Dokumentasjon av ytelser fra utenlandske trygdemyndigheter"),
    ANNET("Annen dokumentasjon")
}

internal class VedleggDeserializer : StdDeserializer<Vedlegg>(Vedlegg::class.java) {

    private val log = getLogger(javaClass)

    @Throws(IOException::class)
    override fun deserialize(p: JsonParser, ctx: DeserializationContext) =
        with(p.codec.readTree(p) as TreeNode) {
            log.trace("Deserialiserer $this")
            when (this) {
                is ArrayNode -> Vedlegg(deler = filterNotNull().map { (it as TextNode).textValue() }
                    .map { UUID.fromString(it) })

                is TextNode -> Vedlegg(deler = listOf(UUID.fromString(textValue())))
                else -> throw IllegalStateException("Ikke-forventet node ${javaClass.simpleName}")
            }
        }
}