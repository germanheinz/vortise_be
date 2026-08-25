/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetProyectosUseCase {
    private final ProyectoRepository proyectoRepository;

    public List<ProyectoDto> execute() {
        return this.proyectoRepository.findAll().stream().map(ProyectoMapper::toSummaryDto).toList();
    }

    public GetProyectosUseCase(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }
}
