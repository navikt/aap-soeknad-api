package no.nav.aap.api.oppslag.graphql

import io.github.resilience4j.retry.annotation.Retry
import org.springframework.graphql.client.GraphQlClient
import org.springframework.web.reactive.function.client.WebClient
import no.nav.aap.api.felles.graphql.GraphQLDefaultErrorHandler
import no.nav.aap.api.felles.graphql.GraphQLErrorHandler
import no.nav.aap.rest.AbstractRestConfig
import no.nav.aap.rest.AbstractWebClientAdapter

abstract class AbstractGraphQLAdapter(client : WebClient, cfg : AbstractRestConfig, val handler : GraphQLErrorHandler = GraphQLDefaultErrorHandler()) :
    AbstractWebClientAdapter(client, cfg) {

    @Retry(name = "graphql")
    protected inline fun <reified T> query(graphQL : GraphQlClient, query : Pair<String, String>, vars : Map<String, List<String>>) =
        runCatching {
            (graphQL
                .documentName(query.first)
                .variables(vars)
                .retrieve(query.second)
                .toEntityList(T::class.java)
                .contextCapture()
                .block() ?: emptyList()).also {
                log.trace("Slo opp liste av {} {}", T::class.java.simpleName, it)
            }
        }.getOrElse {
            handler.handle(it)
        }

    @Retry(name = "graphql")
    protected inline fun <reified T> query(graphQL : GraphQlClient, query : Pair<String, String>, vars : Map<String, String>, info : String) =
        runCatching {
            graphQL
                .documentName(query.first)
                .variables(vars)
                .retrieve(query.second)
                .toEntity(T::class.java)
                .contextCapture()
                .block().also {
                    log.trace("Slo opp {} {}", T::class.java.simpleName, it)
                }
        }.getOrElse { t ->
            log.warn("Query $query feilet. $info", t)
            handler.handle(t)
        }
}