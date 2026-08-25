/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.TareaDto;
import com.vortise.gestion.application.dto.TareaUpdateStatusDto;
import com.vortise.gestion.domain.model.Tarea;
import com.vortise.gestion.domain.repository.TareaRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateTareaStatusUseCase {
    private final TareaRepository tareaRepository;

    public TareaDto execute(Long id, TareaUpdateStatusDto dto) {
        Tarea tarea = this.tareaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + id));
        tarea.setStatus(dto.status());
        if (dto.avancePct() != null) {
            tarea.setAvancePct(dto.avancePct());
        }
        Tarea saved = this.tareaRepository.save(tarea);
        return new TareaDto(saved.getId(), saved.getEntrega().getId(), saved.getFase(), saved.getEtapa(), saved.getSistema(), saved.getNombre(), saved.getStatus(), saved.getAvancePct(), saved.getCantidad(), saved.getCosto(), saved.getFechaInicio(), saved.getFechaFin(), saved.getMes());
    }

    public UpdateTareaStatusUseCase(TareaRepository tareaRepository) {
        this.tareaRepository = tareaRepository;
    }
}
