package com.vortise.gestion.application.dto;

import java.time.LocalDateTime;

public record EmpresaDto(
    Long id,
    String nombre,
    String identificacionFiscal,
    int limiteUsuarios,
    LocalDateTime creadoEn
) {
}
