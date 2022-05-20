package no.nav.aap.api.mellomlagring

import com.google.cloud.storage.BlobId.of
import com.google.cloud.storage.BlobInfo.newBuilder
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.BlobField.CONTENT_TYPE
import com.google.cloud.storage.Storage.BlobField.METADATA
import com.google.cloud.storage.Storage.BlobGetOption.fields
import no.nav.aap.api.felles.Fødselsnummer
import no.nav.aap.api.mellomlagring.Dokumentlager.Companion.FILNAVN
import no.nav.aap.api.mellomlagring.Dokumentlager.Companion.FNR
import no.nav.aap.api.mellomlagring.virus.AttachmentException
import no.nav.aap.api.mellomlagring.virus.VirusScanner
import no.nav.aap.util.LoggerUtil
import no.nav.boot.conditionals.ConditionalOnGCP
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import java.util.UUID.randomUUID

@ConditionalOnGCP
internal class GCPDokumentlager(@Value("\${mellomlagring.bucket:aap-vedlegg}") private val bøtte: String,
                                private val lager: Storage,
                                private val scanner: VirusScanner,
                                private val typeSjekker: TypeSjekker) : Dokumentlager {

    val log = LoggerUtil.getLogger(javaClass)
    override fun lagreDokument(fnr: Fødselsnummer, bytes: ByteArray, contentType: String?, originalFilename: String?) =
        randomUUID().apply {
            typeSjekker.sjekkType(bytes, contentType, originalFilename)
            scanner.scan(bytes, originalFilename)
            lager.create(newBuilder(of(bøtte, key(fnr, this)))
                .setContentType(contentType)
                .setMetadata(mapOf(FILNAVN to originalFilename, FNR to fnr.fnr))
                .build(), bytes)
                .also { log.trace("Lagret $originalFilename med uuid $this") }
        }

    override fun lesDokument(fnr: Fødselsnummer, uuid: UUID) =
        lager.get(bøtte, key(fnr, uuid), fields(METADATA, CONTENT_TYPE))

    override fun slettDokument(fnr: Fødselsnummer, uuid: UUID) =
        lager.delete(of(bøtte, key(fnr, uuid)))

    @Component
    internal class TypeSjekker(@Value("#{\${mellomlager.types :{'application/pdf','image/jpeg','image/png'}}}")
                               private val contentTypes: Set<String>) {

        fun sjekkType(bytes: ByteArray, contentType: String?, originalFilename: String?) {
            with(TIKA.detect(bytes)) {
                if (this != contentType) {
                    throw AttachmentException("Type $this matcher ikke oppgitt $contentType for $originalFilename")
                }
            }
            if (!contentTypes.contains(contentType)) {
                throw AttachmentException("Type $contentType er ikke blant $contentTypes for $originalFilename")
            }
        }
    }

    companion object {
        private val TIKA = Tika()
    }
}