package com.vortise.gestion.application.dto;

import java.time.LocalDate;

public record ObraTareaDto(
    Long id,
    Long proyectoId,
    String nombre,
    String sector,
    String nivel,
    String unidad,
    String rubro,
    Double porcentajeCumplimiento,
    String cumplimiento,
    String causaNoCumplimiento,
    String comentario,
    Double horasOficiales,
    Double horasAyudantes,
    Double cantidadPrevista,
    Double cantidadReal,
    String medidaCorrectiva,
    String descripcion,
    LocalDate fecha,
    Double productividadPresupuesto,
    Double productividadReal,
    Double rendimiento,
    Double desvio
) {
}