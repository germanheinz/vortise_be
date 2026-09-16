package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record EmpresaCreateDto(
    @NotBlank String nombre,
    String identificacionFiscal
) {
}
