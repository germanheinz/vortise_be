/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PresupuestoLineaDto;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class SavePresupuestoLineaUseCase {
    private final PresupuestoLineaRepository presupuestoLineaRepository;
    private final ProyectoRepository proyectoRepository;

    public PresupuestoLineaDto execute(PresupuestoLineaDto dto) {
        Proyecto proyecto = this.proyectoRepository.findById(dto.proyectoId()).orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + dto.proyectoId()));
        PresupuestoLinea linea = PresupuestoLinea.builder().id(dto.id()).proyecto(proyecto).etapa(dto.etapa()).tipoSuperficie(dto.tipoSuperficie()).funcion(dto.funcion()).sistema(dto.sistema()).m2(dto.m2()).hhPorM2(dto.hhPorM2()).precioPorHh(dto.precioPorHh()).precioPorHhUsd(dto.precioPorHhUsd()).mes(dto.mes()).build();
        PresupuestoLinea saved = this.presupuestoLineaRepository.save(linea);
        return this.toDto(saved);
    }

    private PresupuestoLineaDto toDto(PresupuestoLinea l) {
        return new PresupuestoLineaDto(l.getId(), l.getProyecto().getId(), l.getEtapa(), l.getTipoSuperficie(), l.getFuncion(), l.getSistema(), l.getM2(), l.getHhPorM2(), l.getPrecioPorHh(), l.getPrecioPorHhUsd(), l.getMes());
    }

    public SavePresupuestoLineaUseCase(PresupuestoLineaRepository presupuestoLineaRepository, ProyectoRepository proyectoRepository) {
        this.presupuestoLineaRepository = presupuestoLineaRepository;
        this.proyectoRepository = proyectoRepository;
    }
}
