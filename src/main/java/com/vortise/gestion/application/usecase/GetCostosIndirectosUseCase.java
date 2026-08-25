/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CostoIndirectoDto;
import com.vortise.gestion.domain.repository.CostoIndirectoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetCostosIndirectosUseCase {
    private final CostoIndirectoRepository costoIndirectoRepository;

    public List<CostoIndirectoDto> execute(Long proyectoId) {
        return this.costoIndirectoRepository.findByProyectoId(proyectoId).stream().map(c -> new CostoIndirectoDto(c.getId(), c.getProyecto().getId(), c.getConcepto(), c.getMonto(), c.getMes())).toList();
    }

    public GetCostosIndirectosUseCase(CostoIndirectoRepository costoIndirectoRepository) {
        this.costoIndirectoRepository = costoIndirectoRepository;
    }
}
