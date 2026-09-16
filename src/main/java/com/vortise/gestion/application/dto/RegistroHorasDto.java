package com.vortise.gestion.application.dto;

import java.time.OffsetDateTime;

public record RegistroHorasDto(
    Long id,
    Long proyectoId,
    String proyectoNombre,
    String numeroRubro,
    String rubro,
    String subRubro,
    String responsable,
    String etapa,
    OffsetDateTime inicio,
    OffsetDateTime fin,
    Double horas,
    String descripcion,
    Double cantidadPrevista,
    Double cantidadReal,
    Double horasPrevistas,
    Double horasOficiales,
    Double horasAyudantes,
    Double productividadPresupuesto,
    Double productividadPrevista,
    Double productividadReal,
    Double rendimiento,
    Double porcentajeCumplimiento,
    String estadoRendimiento,
    String causaNoCumplimiento,
    String medidaCorrectiva
) {
}