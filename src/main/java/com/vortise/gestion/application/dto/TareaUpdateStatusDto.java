/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotNull
 */
package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.StatusTarea;
import jakarta.validation.constraints.NotNull;

public record TareaUpdateStatusDto(@NotNull StatusTarea status, Integer avancePct) {
}
