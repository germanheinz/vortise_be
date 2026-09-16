package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResponsableEmpresaDto(
    Long id,
    @NotBlank String rol,
    @NotBlank String nombre,
    String email,
    String telefono
) {
}
