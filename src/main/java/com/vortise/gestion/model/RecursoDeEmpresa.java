package com.vortise.gestion.model;

/** A company-scoped configuration record; callers must check ownership before reading or changing it. */
public interface RecursoDeEmpresa {
    Empresa getEmpresa();
}
