package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record RegistroHorasCreateDto(
    Integer numeroRubro,
    @NotBlank String rubro,
    String subRubro,
    String responsable,
    @NotNull OffsetDateTime inicio,
    @NotNull OffsetDateTime fin,
    String descripcion
) {
}