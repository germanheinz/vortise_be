/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EntregaDto;
import com.vortise.gestion.domain.repository.EntregaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetEntregasUseCase {
    private final EntregaRepository entregaRepository;

    public List<EntregaDto> execute(Long proyectoId) {
        return this.entregaRepository.findByProyectoId(proyectoId).stream().map(e -> new EntregaDto(e.getId(), e.getProyecto().getId(), e.getNombre(), e.getOrden())).toList();
    }

    public GetEntregasUseCase(EntregaRepository entregaRepository) {
        this.entregaRepository = entregaRepository;
    }
}
