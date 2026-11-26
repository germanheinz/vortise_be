package com.vortise.gestion.application.helper;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.model.RecursoDeEmpresa;
import java.util.Optional;

/** Loads a company-scoped record and rejects it when it belongs to another company. */
public final class RecursosDeEmpresa {
    private RecursosDeEmpresa() { }

    public static <T extends RecursoDeEmpresa> T propio(Optional<T> recurso, Long empresaId, String noEncontrado, String noPertenece) {
        T item = recurso.orElseThrow(() -> new RecursoNoEncontradoException(noEncontrado));
        if (!empresaId.equals(item.getEmpresa().getId())) throw new SolicitudInvalidaException(noPertenece);
        return item;
    }
}
