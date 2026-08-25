package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RestriccionComentarioCreateDto(
    @NotBlank String autor,
    @NotBlank String mensaje
) {
}