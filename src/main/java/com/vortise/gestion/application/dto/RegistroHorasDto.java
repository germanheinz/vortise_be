package com.vortise.gestion.application.dto;

import java.time.OffsetDateTime;

public record RegistroHorasDto(
    Long id,
    Long proyectoId,
    String proyectoNombre,
    Integer numeroRubro,
    String rubro,
    String subRubro,
    String responsable,
    OffsetDateTime inicio,
    OffsetDateTime fin,
    Double horas,
    String descripcion
) {
}