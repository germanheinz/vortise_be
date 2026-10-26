package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UnidadMetricaEmpresaDto(
    Long id,
    String codigo,
    @NotBlank String nombre
) {
}
