package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PlantaCreateDto(
    @NotBlank String nivel,
    Double superficie,
    Double supCubierta,
    Double supDescubierta,
    Double supCalculada,
    Double coefPlantaTipo
) {
}