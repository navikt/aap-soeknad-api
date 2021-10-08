package no.nav.aap.api.pdl;

import no.nav.aap.api.domain.Navn;
import org.springframework.stereotype.Service;

@Service
public class PDLService {

    private final PDLConnection pdl;

    public PDLService(PDLConnection pdl) {
        this.pdl = pdl;
    }
    public Navn navn() {
        var n =  pdl.navn();
        return new Navn(n.fornavn(), n.mellomnavn(), n.etternavn());
    }
}
