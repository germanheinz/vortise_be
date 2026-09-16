package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotNull;

public record CalendarioLaboralEmpresaDto(
    Long id,
    @NotNull Double horasLunesAViernes,
    @NotNull Double horasSabado,
    boolean trabajaDomingo,
    boolean pagaDobleFeriado,
    boolean pagaDobleSabado,
    boolean pagaDobleNoLaborable
) { }
