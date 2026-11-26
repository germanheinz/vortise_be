package com.vortise.gestion.application.port;

/**
 * Verifies a credential issued by the external identity provider. Implementations must only return identities
 * whose signature, expiry and audience were checked, with an email the provider reports as verified.
 */
public interface IdentidadExternaVerificador {
    record IdentidadVerificada(String sujeto, String email) {
        @Override public String toString() { return "IdentidadVerificada[sujeto=" + sujeto + "]"; }
    }

    /** @throws com.vortise.gestion.application.exception.IdentidadNoVerificadaException if the credential is not valid */
    IdentidadVerificada verificar(String credencial);
}
