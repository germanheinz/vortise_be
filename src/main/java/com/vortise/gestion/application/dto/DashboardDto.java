/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.application.dto;

import com.vortise.gestion.application.dto.AvanceSistemaDto;
import com.vortise.gestion.application.dto.CostoPrecioEntregaDto;
import com.vortise.gestion.application.dto.DashboardKpisDto;
import com.vortise.gestion.application.dto.DocsSistemaDto;
import com.vortise.gestion.application.dto.EstadoDocDto;
import com.vortise.gestion.application.dto.SugerenciaMejoraDto;
import java.util.List;

public record DashboardDto(DashboardKpisDto kpis, List<EstadoDocDto> estadoDocumentos, List<AvanceSistemaDto> avancePorSistema, List<DocsSistemaDto> docsPorSistema, List<CostoPrecioEntregaDto> costoPorEntrega, List<SugerenciaMejoraDto> sugerencias) {
}
