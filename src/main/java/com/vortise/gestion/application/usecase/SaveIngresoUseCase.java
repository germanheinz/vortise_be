/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.IngresoDto;
import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class SaveIngresoUseCase {
    private final IngresoRepository ingresoRepository;
    private final ProyectoRepository proyectoRepository;

    public IngresoDto execute(IngresoDto dto) {
        Proyecto proyecto = this.proyectoRepository.findById(dto.proyectoId()).orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + dto.proyectoId()));
        Ingreso ingreso = Ingreso.builder().id(dto.id()).proyecto(proyecto).concepto(dto.concepto()).monto(dto.monto()).mes(dto.mes()).build();
        Ingreso saved = this.ingresoRepository.save(ingreso);
        return new IngresoDto(saved.getId(), saved.getProyecto().getId(), saved.getConcepto(), saved.getMonto(), saved.getMes());
    }

    public SaveIngresoUseCase(IngresoRepository ingresoRepository, ProyectoRepository proyectoRepository) {
        this.ingresoRepository = ingresoRepository;
        this.proyectoRepository = proyectoRepository;
    }
}
