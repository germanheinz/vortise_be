package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Proyecto;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RestrictionUploadDeadline {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RestrictionUploadDeadline() {
    }

    public static LocalDate deadline(Proyecto proyecto) {
        if (proyecto.getFechaInicio() == null || proyecto.getPedidosDeObraDias() == null) return null;
        try {
            return proyecto.getFechaInicio().plusDays(proyecto.getPedidosDeObraDias());
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    public static void validate(Proyecto proyecto, LocalDate today) {
        LocalDate deadline = deadline(proyecto);
        if (deadline != null && today.isAfter(deadline)) {
            throw new SolicitudInvalidaException("El plazo para cargar restricciones venció el " + DATE_FORMAT.format(deadline));
        }
    }
}