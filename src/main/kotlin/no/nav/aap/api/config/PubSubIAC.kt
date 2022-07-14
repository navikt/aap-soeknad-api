package no.nav.aap.api.config

import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.cloud.storage.NotificationInfo
import com.google.cloud.storage.NotificationInfo.EventType.OBJECT_DELETE
import com.google.cloud.storage.NotificationInfo.EventType.OBJECT_FINALIZE
import com.google.cloud.storage.NotificationInfo.PayloadFormat.JSON_API_V1
import com.google.cloud.storage.Storage
import com.google.iam.v1.Binding
import com.google.iam.v1.GetIamPolicyRequest
import com.google.iam.v1.Policy
import com.google.iam.v1.SetIamPolicyRequest
import com.google.pubsub.v1.ProjectName
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.TopicName
import no.nav.aap.api.søknad.mellomlagring.BucketsConfig
import no.nav.aap.api.søknad.mellomlagring.BucketsConfig.MellomlagringBucketConfig
import no.nav.aap.util.LoggerUtil
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.stereotype.Component

@Component
class PubSubIAC(private val cfgs: BucketsConfig, private val storage: Storage) : InitializingBean {

    private val log = LoggerUtil.getLogger(javaClass)
    override fun afterPropertiesSet() {
        init(cfgs.mellom)
    }

    private fun init(cfg: MellomlagringBucketConfig) =
        with(cfg) {
            if (!harTopic(this)) {
                lagTopic(this)
            }
            else {
                log.trace("Topic ${topicName(this)} finnes allerede i ${projectName()}")
            }
            if (!harSubscription(this)) {
                lagSubscription(this)
            }
            else {
                log.trace("Subscription ${subscriptionName(this)} finnes allerede for topic (${
                    topicName(cfg)
                }")
            }

            setPubSubAdminPolicyForBucketServiceAccountOnTopic(topicFullName(this))  //Idempotent

            if (!harNotifikasjon(this)) {
                lagNotifikasjon(this)
            }
            else {
                log.trace("$navn har allerede en notifikasjon på ${topicName(this)}")
            }
        }

    private fun harTopic(cfg: MellomlagringBucketConfig) =
        listTopics(cfg)
            .map { it.substringAfterLast('/') }
            .contains(cfg.subscription.topic)

    private fun lagTopic(cfg: MellomlagringBucketConfig) =
        TopicAdminClient.create().use { c ->
            c.createTopic(topicName(cfg)).also {
                log.trace("Lagd topic ${it.name}")
            }
        }

    private fun harNotifikasjon(cfg: MellomlagringBucketConfig) =
        cfg.subscription.topic == listNotifikasjoner(cfg)
            .map { it.substringAfterLast('/') }
            .firstOrNull()

    private fun lagNotifikasjon(cfg: MellomlagringBucketConfig) =
        storage.createNotification(cfg.navn,
                NotificationInfo.newBuilder(topicFullName(cfg))
                    .setEventTypes(OBJECT_FINALIZE, OBJECT_DELETE)
                    .setPayloadFormat(JSON_API_V1)
                    .build()).also {
            log.trace("Lagd notifikasjon $it.notificationId}for topic ${topicName(cfg)}")
        }

    private fun setPubSubAdminPolicyForBucketServiceAccountOnTopic(topic: String) =
        TopicAdminClient.create().use { c ->
            with(topic) {
                c.setIamPolicy(SetIamPolicyRequest.newBuilder()
                    .setResource(this)
                    .setPolicy(Policy.newBuilder(c.getIamPolicy(GetIamPolicyRequest.newBuilder()
                        .setResource(this).build())).addBindings(Binding.newBuilder()
                        .setRole("roles/pubsub.publisher")
                        .addMembers("serviceAccount:${storage.getServiceAccount(cfgs.id).email}")
                        .build()).build())
                    .build()).also { log.trace("Ny policy er ${it.bindingsList}") }
            }
        }

    private fun listNellomlagerNotifikasjoner() = listNotifikasjoner(cfgs.mellom)

    private fun listNotifikasjoner(cfg: MellomlagringBucketConfig) =
        storage.listNotifications(cfg.navn)
            .map { it.topic }

    private fun listMellomlagerTopics() = listTopics(cfgs.mellom)

    fun listTopics(cfg: MellomlagringBucketConfig) =
        TopicAdminClient.create().use { c ->
            c.listTopics(projectName())
                .iterateAll()
                .map { it.name }
        }

    private fun listMellomlagerSubscriptions() = listSubscriptions(cfgs.mellom)

    private fun listSubscriptions(cfg: MellomlagringBucketConfig) =
        TopicAdminClient.create().use { c ->
            c.listTopicSubscriptions(topicName(cfg))
                .iterateAll()
        }

    private fun harSubscription(cfg: MellomlagringBucketConfig) =
        listSubscriptions(cfg)
            .map { it.substringAfterLast('/') }
            .contains(cfg.subscription.navn)

    private fun lagSubscription(cfg: MellomlagringBucketConfig) =
        SubscriptionAdminClient.create().use { c ->
            c.createSubscription(subscriptionName(cfg),
                    topicName(cfg),
                    PushConfig.getDefaultInstance(),
                    10).also {
                log.trace("Lagd pull subscription ${it.name}")
            }
        }

    private fun subscriptionName(cfg: MellomlagringBucketConfig) = SubscriptionName.of(cfgs.id, cfg.subscription.navn)
    private fun projectName() = ProjectName.of(cfgs.id)
    private fun topicName(cfg: MellomlagringBucketConfig) = TopicName.of(cfgs.id, cfg.subscription.topic)
    private fun topicFullName(cfg: MellomlagringBucketConfig) = topicName(cfg).toString()

    @Component
    @Endpoint(id = "iac")
    class IACEndpoint(private val iac: PubSubIAC) {
        @ReadOperation
        fun iacOperation() =
            with(iac) {
                mapOf("topics" to listMellomlagerTopics(),
                        "subscriptions" to listMellomlagerSubscriptions(),
                        "notifications" to listNellomlagerNotifikasjoner())
            }

        @ReadOperation
        fun iacByName(@Selector name: String) =
            with(iac) {
                when (name) {
                    "topics" -> mapOf("topics" to listMellomlagerTopics())
                    "subscriptions" -> mapOf("subscriptions" to listMellomlagerSubscriptions())
                    "notifications" -> mapOf("notifications" to listNellomlagerNotifikasjoner())
                    else -> iacOperation()
                }
            }
    }
}