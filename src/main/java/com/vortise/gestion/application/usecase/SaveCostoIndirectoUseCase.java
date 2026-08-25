/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CostoIndirectoDto;
import com.vortise.gestion.domain.model.CostoIndirecto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.CostoIndirectoRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class SaveCostoIndirectoUseCase {
    private final CostoIndirectoRepository costoIndirectoRepository;
    private final ProyectoRepository proyectoRepository;

    public CostoIndirectoDto execute(CostoIndirectoDto dto) {
        Proyecto proyecto = this.proyectoRepository.findById(dto.proyectoId()).orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + dto.proyectoId()));
        CostoIndirecto c = CostoIndirecto.builder().id(dto.id()).proyecto(proyecto).concepto(dto.concepto()).monto(dto.monto()).mes(dto.mes()).build();
        CostoIndirecto saved = this.costoIndirectoRepository.save(c);
        return new CostoIndirectoDto(saved.getId(), saved.getProyecto().getId(), saved.getConcepto(), saved.getMonto(), saved.getMes());
    }

    public SaveCostoIndirectoUseCase(CostoIndirectoRepository costoIndirectoRepository, ProyectoRepository proyectoRepository) {
        this.costoIndirectoRepository = costoIndirectoRepository;
        this.proyectoRepository = proyectoRepository;
    }
}
