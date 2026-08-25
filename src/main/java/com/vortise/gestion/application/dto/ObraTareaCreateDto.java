package com.vortise.gestion.application.dto;

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
    String comentario
) {
}