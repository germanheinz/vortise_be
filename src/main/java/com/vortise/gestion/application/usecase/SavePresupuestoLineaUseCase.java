/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PresupuestoLineaDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavePresupuestoLineaUseCase {
    private final PresupuestoLineaRepository presupuestoLineaRepository;
    private final ProyectoRepository proyectoRepository;

    @Transactional
    public PresupuestoLineaDto execute(PresupuestoLineaDto dto) {
        Proyecto proyecto = this.proyectoRepository.findById(dto.proyectoId()).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado: " + dto.proyectoId()));
        PresupuestoLinea linea;
        if (dto.id() == null) {
            linea = PresupuestoLinea.builder().proyecto(proyecto).build();
        } else {
            linea = presupuestoLineaRepository.findById(dto.id())
                .filter(existing -> existing.getProyecto() != null && proyecto.getId().equals(existing.getProyecto().getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado"));
        }
        linea.setEtapa(dto.etapa());
        linea.setTipoSuperficie(dto.tipoSuperficie());
        linea.setFuncion(dto.funcion());
        linea.setSistema(dto.sistema());
        linea.setM2(dto.m2());
        linea.setHhPorM2(dto.hhPorM2());
        linea.setPrecioPorHh(dto.precioPorHh());
        linea.setPrecioPorHhUsd(dto.precioPorHhUsd());
        linea.setMes(dto.mes());
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
