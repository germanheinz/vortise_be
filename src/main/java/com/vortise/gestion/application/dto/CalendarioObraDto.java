package com.vortise.gestion.application.dto;

/** Calendario laboral de una obra: qué fines de semana trabaja y cuándo se paga doble (spec 016). */
public record CalendarioObraDto(boolean trabajaSabado, boolean trabajaDomingo, boolean pagaDobleFeriado, boolean pagaDobleNoLaborable) { }
