/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RendimientoDto;
import com.vortise.gestion.domain.repository.RendimientoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetRendimientosUseCase {
    private final RendimientoRepository rendimientoRepository;

    public List<RendimientoDto> execute() {
        return this.rendimientoRepository.findAll().stream().map(r -> new RendimientoDto(r.getId(), r.getFase(), r.getEtapa(), r.getSistema(), r.getRol(), r.getHhPorM2(), r.getPrecioPorHh())).toList();
    }

    public GetRendimientosUseCase(RendimientoRepository rendimientoRepository) {
        this.rendimientoRepository = rendimientoRepository;
    }
}
