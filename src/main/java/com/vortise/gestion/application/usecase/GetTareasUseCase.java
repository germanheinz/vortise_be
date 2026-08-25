/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.TareaDto;
import com.vortise.gestion.domain.repository.TareaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetTareasUseCase {
    private final TareaRepository tareaRepository;

    public List<TareaDto> execute(Long proyectoId) {
        return this.tareaRepository.findByEntregaProyectoId(proyectoId).stream().map(t -> new TareaDto(t.getId(), t.getEntrega().getId(), t.getFase(), t.getEtapa(), t.getSistema(), t.getNombre(), t.getStatus(), t.getAvancePct(), t.getCantidad(), t.getCosto(), t.getFechaInicio(), t.getFechaFin(), t.getMes())).toList();
    }

    public GetTareasUseCase(TareaRepository tareaRepository) {
        this.tareaRepository = tareaRepository;
    }
}
