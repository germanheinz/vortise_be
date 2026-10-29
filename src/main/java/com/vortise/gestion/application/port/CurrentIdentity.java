package com.vortise.gestion.application.port;

/** Application identity, independent of JWT/Spring Security transport details. */
public interface CurrentIdentity {
    Long empresaId();
    Long usuarioId();
    void requireEmpresa(Long empresaId);
    boolean canAccessEmpresa(Long empresaId);
    boolean isAdministrator();
    boolean isSuperAdmin();
}
