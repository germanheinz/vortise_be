package com.vortise.gestion.infrastructure.security;

/**
 * Role expressions for {@code @PreAuthorize}. The guard of each endpoint comes from the access matrix
 * (spec 014); reads carry no role guard. A JEFE_DE_OBRA is further limited to its assigned sites by
 * {@link TenantAccessInterceptor}.
 */
public final class Guards {
    /** Gerencia/Coordinación only (Acceso 2 plus the owner). Strictly ADMINISTRADOR. */
    public static final String SOLO_ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
    /** Acceso 2 on endpoints that were already open to the invited team (COLABORADOR is unchanged). */
    public static final String GERENTE = "hasAnyRole('ADMINISTRADOR','COLABORADOR')";
    /** Acceso 2 and Acceso 3 (Jefatura, on assigned sites) on endpoints that were open to the invited team. */
    public static final String OBRA = "hasAnyRole('ADMINISTRADOR','COLABORADOR','JEFE_DE_OBRA')";
    /** Certificate writes were already limited to ADMINISTRADOR and JEFE_DE_OBRA. */
    public static final String CERTIFICADOS = "hasAnyRole('ADMINISTRADOR','JEFE_DE_OBRA')";

    private Guards() { }
}
