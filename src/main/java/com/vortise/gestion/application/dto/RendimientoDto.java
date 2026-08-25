/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotNull
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotNull;

public record RendimientoDto(Long id, String fase, String etapa, String sistema, String rol, @NotNull Double hhPorM2, @NotNull Double precioPorHh) {
}
