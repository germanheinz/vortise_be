package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UnidadMetricaEmpresaDto(
    Long id,
    @NotBlank String codigo,
    @NotBlank String nombre
) {
}
