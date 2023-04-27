package no.nav.aap.api.søknad.arkiv

import org.springframework.stereotype.Service
import no.nav.aap.api.søknad.model.Innsending
import no.nav.aap.api.søknad.model.StandardEttersending
import no.nav.aap.api.søknad.model.Søker

@Service
class ArkivFordeler(private val arkiv : ArkivClient, private val generator : ArkivJournalpostGenerator) {

    fun fordel(innsending : Innsending, søker : Søker) = arkiv.arkiver(generator.journalpostFra(innsending, søker))
    fun fordel(ettersending : StandardEttersending, søker : Søker, routing : Boolean) = arkiv.arkiver(generator.journalpostFra(ettersending, søker, routing))
}