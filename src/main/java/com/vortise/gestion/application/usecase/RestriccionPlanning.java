package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import java.time.LocalDate;

final class RestriccionPlanning {
    private RestriccionPlanning() { }

    static String area(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 120) throw new SolicitudInvalidaException("Área admite hasta 120 caracteres");
        return normalized;
    }

    static void validateDates(LocalDate solicitud, LocalDate maxima, LocalDate real) {
        if (solicitud != null && maxima != null && maxima.isBefore(solicitud)) {
            throw new SolicitudInvalidaException("La fecha máxima no puede ser anterior a la solicitud");
        }
        if (solicitud != null && real != null && real.isBefore(solicitud)) {
            throw new SolicitudInvalidaException("La entrega real no puede ser anterior a la solicitud");
        }
    }
}