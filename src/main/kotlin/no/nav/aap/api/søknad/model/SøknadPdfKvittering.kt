package no.nav.aap.api.søknad.model

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDateTime

data class SøknadPdfKvittering(
        val temaer: List<Tema>,
        val mottattdato: LocalDateTime
) {
}

data class Tema(val type: String,
                val overskrift: String?,
                val underblokker: List<Blokk>)
data class Blokk(val type: String,
                  val overskrift: String?,
                val tabellrader: List<Blokk>?,
                 val tekst: String?,
                 val tittel: String?,
                 val punkter: List<String>?,
                 val felter: List<String>?,
                 val indent: Boolean?,
                 val verdi: String?)