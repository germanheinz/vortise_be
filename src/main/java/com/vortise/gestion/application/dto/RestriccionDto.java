package com.vortise.gestion.application.dto;

import java.time.LocalDate;

public record RestriccionDto(
    Long id,
    Long proyectoId,
    String proyectoNombre,
    String restriccion,
    String tipo,
    String sector,
    String nivel,
    String unidad,
    String rubro,
    String responsable,
    LocalDate fechaSolicitud,
    LocalDate fechaMaximaEntrega,
    LocalDate fechaRealEntrega,
    String carga,
    String prioridad,
    String estado,
    Integer orden
) {
}