package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    String estado,
    @Size(max = 120) String area
) {
    public RestriccionCreateDto(String restriccion, Long rubroId, String tipo, String sector,
            String nivel, String unidad, String rubro, String responsable, LocalDate fechaSolicitud,
            LocalDate fechaMaximaEntrega, LocalDate fechaRealEntrega, String carga, String prioridad, String estado) {
        this(restriccion, rubroId, tipo, sector, nivel, unidad, rubro, responsable,
            fechaSolicitud, fechaMaximaEntrega, fechaRealEntrega, carga, prioridad, estado, null);
    }
}