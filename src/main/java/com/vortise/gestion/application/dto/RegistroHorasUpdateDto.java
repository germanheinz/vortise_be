package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record RegistroHorasUpdateDto(
    String numeroRubro,
    @NotBlank String rubro,
    String subRubro,
    String responsable,
    String etapa,
    @NotNull OffsetDateTime inicio,
    @NotNull OffsetDateTime fin,
    String descripcion,
    Double cantidadPrevista,
    Double cantidadReal,
    Double horasPrevistas,
    Double horasOficiales,
    Double horasAyudantes,
    String causaNoCumplimiento,
    String medidaCorrectiva
) {
}