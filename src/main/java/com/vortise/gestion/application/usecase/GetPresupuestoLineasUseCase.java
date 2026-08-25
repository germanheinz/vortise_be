/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PresupuestoLineaDto;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetPresupuestoLineasUseCase {
    private final PresupuestoLineaRepository presupuestoLineaRepository;

    public List<PresupuestoLineaDto> execute(Long proyectoId) {
        return this.presupuestoLineaRepository.findByProyectoId(proyectoId).stream().map(l -> new PresupuestoLineaDto(l.getId(), l.getProyecto().getId(), l.getEtapa(), l.getTipoSuperficie(), l.getFuncion(), l.getSistema(), l.getM2(), l.getHhPorM2(), l.getPrecioPorHh(), l.getPrecioPorHhUsd(), l.getMes())).toList();
    }

    public GetPresupuestoLineasUseCase(PresupuestoLineaRepository presupuestoLineaRepository) {
        this.presupuestoLineaRepository = presupuestoLineaRepository;
    }
}
