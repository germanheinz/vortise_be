package com.vortise.gestion.application.port;

/** Application identity, independent of JWT/Spring Security transport details. */
public interface CurrentIdentity {
    Long empresaId();
    default boolean isCollaborator() { return false; }
    default java.util.List<Long> empresasAccesibles() { return java.util.List.of(empresaId()); }
    Long usuarioId();
    void requireEmpresa(Long empresaId);
    boolean canAccessEmpresa(Long empresaId);
    boolean isAdministrator();
    boolean isSuperAdmin();
}
