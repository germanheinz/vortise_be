/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RendimientoDto;
import com.vortise.gestion.domain.model.Rendimiento;
import com.vortise.gestion.domain.repository.RendimientoRepository;
import org.springframework.stereotype.Service;

@Service
public class SaveRendimientoUseCase {
    private final RendimientoRepository rendimientoRepository;

    public RendimientoDto execute(RendimientoDto dto) {
        Rendimiento r = Rendimiento.builder().id(dto.id()).fase(dto.fase()).etapa(dto.etapa()).sistema(dto.sistema()).rol(dto.rol()).hhPorM2(dto.hhPorM2()).precioPorHh(dto.precioPorHh()).build();
        Rendimiento saved = this.rendimientoRepository.save(r);
        return new RendimientoDto(saved.getId(), saved.getFase(), saved.getEtapa(), saved.getSistema(), saved.getRol(), saved.getHhPorM2(), saved.getPrecioPorHh());
    }

    public SaveRendimientoUseCase(RendimientoRepository rendimientoRepository) {
        this.rendimientoRepository = rendimientoRepository;
    }
}
