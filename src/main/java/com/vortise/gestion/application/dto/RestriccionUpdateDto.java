package com.vortise.gestion.application.dto;

import java.time.LocalDate;

public record RestriccionUpdateDto(
    String prioridad,
    String estado,
    Integer orden,
    String area,
    String responsable,
    LocalDate fechaSolicitud,
    LocalDate fechaMaximaEntrega,
    LocalDate fechaRealEntrega,
    boolean actualizarFechas
) {
    public RestriccionUpdateDto(String prioridad, String estado, Integer orden) {
        this(prioridad, estado, orden, null, null, null, null, null, false);
    }
}