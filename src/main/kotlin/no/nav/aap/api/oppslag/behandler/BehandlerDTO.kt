package no.nav.aap.api.oppslag.behandler

import no.nav.aap.api.felles.Fødselsnummer
import no.nav.aap.api.felles.Navn
import no.nav.aap.api.felles.OrgNummer
import no.nav.aap.api.oppslag.behandler.Behandler.BehandlerType
import no.nav.aap.api.oppslag.behandler.Behandler.KontaktInformasjon

data class BehandlerDTO(
        val type: BehandlerType,
        val behandlerRef: String,
        val fnr: Fødselsnummer?,
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val orgnummer: OrgNummer?,
        val kontor: String?,
        val adresse: String?,
        val postnummer: String?,
        val poststed: String?,
        val telefon: String?)


data class Behandler(val type: BehandlerType,
                     val navn: Navn,
                     val kontaktinformasjon: KontaktInformasjon) {
    enum class BehandlerType {
        FASTLEGE
    }
    data class KontaktInformasjon(val behandlerRef: String,
                                  val kontor: String?,
                                  val orgnummer: OrgNummer?,
                                  val adresse: String?,
                                  val postnr: String?,
                                  val poststed: String?,
                                  var telefon: String?)
}
fun BehandlerDTO.tilBehandler() = Behandler(type,Navn(fornavn,mellomnavn,etternavn),
        KontaktInformasjon(behandlerRef,kontor,orgnummer,adresse,postnummer,poststed,telefon))