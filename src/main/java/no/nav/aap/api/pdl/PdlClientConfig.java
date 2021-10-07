package no.nav.aap.api.pdl;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.spring.webclient.boot.GraphQLWebClient;
import no.nav.aap.api.tokenx.TokenXConfigMatcher;
import no.nav.aap.api.tokenx.TokenXFilterFunction;
import no.nav.aap.api.util.MDCUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import java.util.Optional;

import static no.nav.aap.api.util.MDCUtil.NAV_CALL_ID;
import static no.nav.aap.api.util.MDCUtil.NAV_CALL_ID1;
import static no.nav.aap.api.util.MDCUtil.NAV_CONSUMER_ID;
import static no.nav.aap.api.util.MDCUtil.callId;

@Configuration
public class PdlClientConfig {

    public static final String PDL_USER = "PDL";
    public static final String STS = "STS";
    private static final String TEMA = "TEMA";
    private static final Logger LOG = LoggerFactory.getLogger(PdlClientConfig.class);
    private static final String AAP = "AAP";
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    @Value("${spring.application.name:aap-soknad-api}")
    private String consumer;

    private String consumerId() {
        return Optional.ofNullable(MDCUtil.consumerId())
                .orElse(consumer);
    }

    private ExchangeFilterFunction correlatingFilterFunction() {
        return (req, next) -> next.exchange(ClientRequest.from(req)
                .header(NAV_CONSUMER_ID, consumerId())
                .header(NAV_CALL_ID, callId())
                .header(NAV_CALL_ID1, callId())
                .build());
    }

    private static ExchangeFilterFunction temaFilterFunction() {
        return (req, next) -> next.exchange(ClientRequest.from(req)
                .header(TEMA, AAP)
                .build());
    }

    @Qualifier(PDL_USER)
    @Bean
    public WebClient webClientPDL(Builder builder, PDLConfig cfg, TokenXFilterFunction tokenXFilterFunction) {
        return builder
                .baseUrl(cfg.getBaseUri().toString())
                .filter(correlatingFilterFunction())
                .filter(temaFilterFunction())
                .filter(tokenXFilterFunction)
                .build();
    }

    @Qualifier(PDL_USER)
    @Bean
    public GraphQLWebClient pdlWebClient(@Qualifier(PDL_USER) WebClient client, ObjectMapper mapper) {
        return GraphQLWebClient.newInstance(client, mapper);
    }

    @Bean
    public TokenXConfigMatcher configMatcher() {
        return (cfgs, req) -> {
            String host = req.getHost();
            LOG.trace("Oppslag token X konfig for {}", host);
            var cfg = Optional.ofNullable(cfgs.getRegistration().get(host.split("\\.")[0]));
            cfg.ifPresentOrElse(
                    c-> LOG.trace("Oppslag token X konfig for {} OK", host),
                    () -> LOG.trace("Oppslag token X konfig for {} fant ingenting", host));
           return cfg;
        };
    }
}
