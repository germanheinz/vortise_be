package com.vortise.gestion.application.dto;

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
    String comentario
) {
}