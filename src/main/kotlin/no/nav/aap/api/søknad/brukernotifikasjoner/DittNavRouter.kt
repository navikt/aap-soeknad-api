package no.nav.aap.api.søknad.brukernotifikasjoner

import no.nav.aap.api.felles.Fødselsnummer
import no.nav.aap.api.felles.SkjemaType
import no.nav.aap.api.søknad.brukernotifikasjoner.DittNavCallbacks.DittNavBeskjedCallback
import no.nav.aap.api.søknad.brukernotifikasjoner.DittNavCallbacks.DittNavDoneCallback
import no.nav.aap.api.søknad.brukernotifikasjoner.DittNavCallbacks.DittNavOppgaveCallback
import no.nav.aap.api.søknad.brukernotifikasjoner.DittNavConfig.TopicConfig
import no.nav.aap.util.LoggerUtil
import no.nav.aap.util.MDCUtil.callId
import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.boot.conditionals.EnvUtil.CONFIDENTIAL
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaOperations
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri
import java.time.LocalDateTime.now
import java.time.ZoneOffset.UTC

@ConditionalOnGCP
class DittNavRouter(private val dittNav: KafkaOperations<NokkelInput, Any>,
                    private val cfg: DittNavConfig,
                    private val repos: DittNavRepositories) {

    private val log = LoggerUtil.getLogger(javaClass)

    fun opprettBeskjed(fnr: Fødselsnummer, type: SkjemaType) =
        if (cfg.beskjed.enabled) {
            with(nøkkelInput(fnr, type.name, callId(), "beskjed")) {
                dittNav.send(ProducerRecord(cfg.beskjed.topic,
                        this, beskjed(cfg.beskjed, type, "Mottatt ${type.tittel}")))
                    .addCallback(DittNavBeskjedCallback(this, repos.beskjed))
            }
        }
        else {
            log.info("Sender ikke beskjed til Ditt Nav")
        }

    fun opprettOppgave(fnr: Fødselsnummer, type: SkjemaType, tekst: String) =
        if (cfg.oppgave.enabled) {
            with(nøkkelInput(fnr, type.name, callId(), "oppgave")) {
                dittNav.send(ProducerRecord(cfg.oppgave.topic, this, oppgave(cfg.oppgave, type, tekst)))
                    .addCallback(DittNavOppgaveCallback(this, repos.oppgave))
            }
        }
        else {
            log.info("Sender ikke oppgave til Ditt Nav")
        }

    fun done(fnr: Fødselsnummer, type: SkjemaType, eventId: String) =
        if (cfg.done.enabled) {
            with(nøkkelInput(fnr, type.name, eventId, "done")) {
                dittNav.send(ProducerRecord(cfg.done.topic, this, done()))
                    .addCallback(DittNavDoneCallback(this, repos.oppgave))
            }
        }
        else {
            log.info("Sender ikke done til Ditt Nav")
        }

    private fun beskjed(cfg: TopicConfig, type: SkjemaType, tekst: String) =
        with(cfg) {
            BeskjedInputBuilder()
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withTidspunkt(now(UTC))
                .withSynligFremTil(now(UTC).plus(varighet))
                .withLink(replaceWith("/aap/${type.name}"))
                .withTekst(tekst)
                .withEksternVarsling(eksternVarsling)
                .withPrefererteKanaler(*preferertekanaler.toTypedArray())
                .build()
        }

    private fun oppgave(cfg: TopicConfig, type: SkjemaType, tekst: String) =
        with(cfg) {
            OppgaveInputBuilder()
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withTidspunkt(now(UTC))
                .withSynligFremTil(now(UTC).plus(varighet))
                .withLink(replaceWith("/aap/${type.name}"))
                .withTekst(tekst)
                .withEksternVarsling(eksternVarsling)
                .withPrefererteKanaler(*preferertekanaler.toTypedArray())
                .build()
        }

    private fun done() =
        DoneInputBuilder()
            .withTidspunkt(now(UTC))
            .build()

    private fun replaceWith(replacement: String) =
        fromCurrentRequestUri().replacePath(replacement).build().toUri().toURL()

    private fun nøkkelInput(fnr: Fødselsnummer, grupperingId: String, eventId: String, type: String) =
        with(cfg) {
            NokkelInputBuilder()
                .withFodselsnummer(fnr.fnr)
                .withEventId(eventId)
                .withGrupperingsId(grupperingId)
                .withAppnavn(app)
                .withNamespace(namespace)
                .build()
                .also {
                    log.info(CONFIDENTIAL, "Key for Ditt Nav $type er $this")
                }
        }
}