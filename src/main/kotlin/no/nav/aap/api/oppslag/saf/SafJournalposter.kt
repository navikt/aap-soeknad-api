package no.nav.aap.api.oppslag.saf

import java.time.LocalDateTime

    data class SafJournalposter(val journalposter: List<SafJournalpost>) {

        data class SafJournalpost(val journalpostId: String,
                                  val journalposttype: SafJournalpostType,
                                  val journalstatus: SafJournalStatus,
                                  val tittel: String?,
                                  val relevanteDatoer: List<SafRelevantDato>,
                                  val sak: SafSak?,
                                  val dokumenter: List<SafDokumentInfo>) {

            enum class SafJournalpostType {
                I,U,N
            }
            
            enum class SafJournalStatus {
                MOTTATT,
                JOURNALFOERT
                ,EKSPEDERT,
                FERDIGSTILT,
                UNDER_ARBEID,
                FEILREGISTRERT,
                UTGAAR,
                AVBRUTT,
                UKJENT_BRUKER,
                RESERVERT,
                OPPLASTING_DOKUMENT,
                UKJENT
            }

            data class SafRelevantDato(val dato: LocalDateTime, val datotype: SafDatoType) {

                enum class SafDatoType {
                    DATO_OPPRETTET,
                    DATO_SENDT_PRINT,
                    DATO_EKSPEDERT,
                    DATO_JOURNALFOERT,
                    DATO_REGISTRERT,
                    DATO_AVS_RETUR,
                    DATO_DOKUMENT
                }
            }

            data class SafSak(val fagsakId: String?,val fahsaksystem: String?,val sakstype: SafSakstype) {

                enum class SafSakstype  {
                    GENERELL_SAK,FAGSAK
                }
            }

            data class SafDokumentInfo(val dokumentInfoId: String, val brevkode: String?, val tittel: String?, val dokumentvarianter: List<SafDokumentVariant>){

                data class SafDokumentVariant(val variantformat: Format, val filtype: SafFiltype, val brukerHarTilgang: Boolean) {

                    enum class Format {
                        ARKIV,SLADDET
                    }

                    enum class SafFiltype {
                        PDF,JPG,PNG
                    }
                }
            }
        }
    }