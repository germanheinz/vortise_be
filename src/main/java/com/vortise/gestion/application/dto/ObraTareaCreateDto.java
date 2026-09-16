package com.vortise.gestion.application.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record ObraTareaCreateDto(
    @NotBlank String nombre,
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
    LocalDate fecha
) {
}