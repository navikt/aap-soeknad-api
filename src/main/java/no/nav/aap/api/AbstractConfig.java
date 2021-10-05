package no.nav.aap.api;

import no.nav.aap.api.util.URIUtil;

import java.net.URI;

public class AbstractConfig {
    private final URI baseUri;
    private final String pingPath;
    private final boolean enabled;

    protected AbstractConfig(URI baseUri, String pingPath, boolean enabled) {
        this.baseUri = baseUri;
        this.pingPath = pingPath;
        this.enabled = enabled;
    }

    public URI pingEndpoint() {
        return URIUtil.uri(baseUri, pingPath);
    }

    public String getPingPath() {
        return pingPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public String name() {
        return baseUri.getHost();
    }
}
