/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.application.dto;

import com.vortise.gestion.application.dto.CashflowMesDto;
import java.util.List;

public record CashflowDto(Long proyectoId, String proyectoNombre, List<CashflowMesDto> meses) {
}
