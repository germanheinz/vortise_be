/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.IngresoDto;
import com.vortise.gestion.domain.repository.IngresoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetIngresosUseCase {
    private final IngresoRepository ingresoRepository;

    public List<IngresoDto> execute(Long proyectoId) {
        return this.ingresoRepository.findByProyectoId(proyectoId).stream().map(i -> new IngresoDto(i.getId(), i.getProyecto().getId(), i.getConcepto(), i.getMonto(), i.getMes())).toList();
    }

    public GetIngresosUseCase(IngresoRepository ingresoRepository) {
        this.ingresoRepository = ingresoRepository;
    }
}
