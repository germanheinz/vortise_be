package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RestriccionCreateDto(
    @NotBlank String restriccion,
    @NotNull Long rubroId,
    String tipo,
    String sector,
    String nivel,
    String unidad,
    String rubro,
    String responsable,
    LocalDate fechaSolicitud,
    LocalDate fechaMaximaEntrega,
    LocalDate fechaRealEntrega,
    String carga,
    String prioridad,
    String estado
) {
}