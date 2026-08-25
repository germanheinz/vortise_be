package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RestriccionCreateDto(
    @NotBlank String restriccion,
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