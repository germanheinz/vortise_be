/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.StatusTarea;
import java.time.LocalDate;

public record TareaDto(Long id, Long entregaId, String fase, String etapa, String sistema, String nombre, StatusTarea status, Integer avancePct, Double cantidad, Double costo, LocalDate fechaInicio, LocalDate fechaFin, Integer mes) {
}
