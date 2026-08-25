/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PresupuestoLineaDto(Long id, Long proyectoId, @NotBlank String etapa, @NotBlank String tipoSuperficie, @NotBlank String funcion, String sistema, Double m2, Double hhPorM2, Double precioPorHh, Double precioPorHhUsd, Integer mes) {
    public double costoArs() {
        if (this.m2 == null || this.hhPorM2 == null || this.precioPorHh == null) {
            return 0.0;
        }
        return this.m2 * this.hhPorM2 * this.precioPorHh;
    }

    public double honorariosUsd() {
        if (this.m2 == null || this.hhPorM2 == null || this.precioPorHhUsd == null) {
            return 0.0;
        }
        return this.m2 * this.hhPorM2 * this.precioPorHhUsd;
    }
}
