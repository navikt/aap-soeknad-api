package no.nav.aap.api.søknad.minside

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.aap.api.config.BeanConfig.AbstractKafkaHealthIndicator
import no.nav.aap.api.søknad.minside.EksternNotifikasjonStatusKonsument.Companion.DOKNOTIFIKASJON
import no.nav.aap.api.søknad.minside.EksternNotifikasjonStatusKonsument.Companion.FEILET
import no.nav.aap.api.søknad.minside.EksternNotifikasjonStatusKonsument.Companion.FERDIGSTILT
import no.nav.aap.api.søknad.minside.EksternNotifikasjonStatusKonsument.Companion.NOTIFIKASJON_SENDT
import no.nav.aap.health.AbstractPingableHealthIndicator
import no.nav.aap.util.LoggerUtil.getLogger
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS
import org.springframework.stereotype.Component

@Configuration
class MinSideBeanConfig(@Value("\${spring.application.name}") private val appNavn: String) {
    private val log = getLogger(javaClass)

    @Component
    class MinsidePingable(val admin: KafkaAdmin, val p: KafkaProperties, val cfg: MinSideConfig) : AbstractKafkaHealthIndicator(admin,p.bootstrapServers,cfg)

    @Bean
    fun minsideHealthIndicator(adapter: MinsidePingable) = object : AbstractPingableHealthIndicator(adapter) {}
    @Bean
    fun minSideKafkaOperations(props: KafkaProperties) =
        KafkaTemplate(DefaultKafkaProducerFactory<NokkelInput, Any>(props.buildProducerProperties()
            .apply {
                put(KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
                put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            }))

    @Bean(DOKNOTIFIKASJON)
    fun dokNotifikasjonListenerContainerFactory(p: KafkaProperties) =
        ConcurrentKafkaListenerContainerFactory<String, DoknotifikasjonStatus>().apply {
            consumerFactory =
                DefaultKafkaConsumerFactory(p.buildConsumerProperties().apply {
                    put(KEY_DESERIALIZER_CLASS, StringDeserializer::class.java)
                    put(VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer::class.java)
                    put(SPECIFIC_AVRO_READER_CONFIG, true)
                    setRecordFilterStrategy(::recordFilterStrategy)
                })
        }

    private fun recordFilterStrategy(payload: ConsumerRecord<String, DoknotifikasjonStatus>) =
        with(payload.value()) {
            when (bestillerId) {
                appNavn -> {
                    when (status) {
                        FERDIGSTILT -> !melding.contains(NOTIFIKASJON_SENDT)

                        FEILET ->
                            true.also {
                                log.warn("Ekstern notifikasjon feilet for bestillingid $bestillingsId, ($melding)")
                            }

                        else ->
                            true.also {
                                log.trace("Ekstern notifikasjon status $status filtrert vekk for bestillingid $bestillingsId")
                            }
                    }
                }
                else -> true
            }
        }

  }