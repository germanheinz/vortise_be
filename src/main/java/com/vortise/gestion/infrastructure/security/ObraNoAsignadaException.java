package com.vortise.gestion.infrastructure.security;

import org.springframework.security.access.AccessDeniedException;

/** A JEFE_DE_OBRA tried to write on a site that is not among its assigned sites. */
public class ObraNoAsignadaException extends AccessDeniedException {
    public static final String CODE = "OBRA_NO_ASIGNADA";

    public ObraNoAsignadaException() {
        super("No tenés permiso para modificar esta obra.");
    }
}
