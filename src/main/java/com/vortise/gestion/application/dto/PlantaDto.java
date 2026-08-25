/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 */
package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PlantaDto(Long id, Long proyectoId, @NotBlank String nivel, Double superficie, Double supCubierta, Double supDescubierta, Double supCalculada, Double coefPlantaTipo) {
}
