package no.nav.aap.api.tokenx;

import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;

import java.net.URI;
import java.util.Optional;

public interface TokenXConfigMatcher {
    Optional<ClientProperties> findProperties(ClientConfigurationProperties configs, URI uri);

}
