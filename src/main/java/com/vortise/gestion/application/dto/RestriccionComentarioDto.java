package com.vortise.gestion.application.dto;

import java.time.LocalDateTime;

public record RestriccionComentarioDto(
    Long id,
    Long restriccionId,
    String autor,
    String mensaje,
    LocalDateTime creadoEn
) {
}