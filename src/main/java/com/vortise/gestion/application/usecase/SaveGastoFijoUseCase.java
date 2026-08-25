/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.GastoFijoDto;
import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import org.springframework.stereotype.Service;

@Service
public class SaveGastoFijoUseCase {
    private final GastoFijoRepository gastoFijoRepository;

    public GastoFijoDto execute(GastoFijoDto dto) {
        GastoFijo g = GastoFijo.builder().id(dto.id()).concepto(dto.concepto()).monto(dto.monto()).categoria(dto.categoria()).mes(dto.mes()).build();
        GastoFijo saved = this.gastoFijoRepository.save(g);
        return new GastoFijoDto(saved.getId(), saved.getConcepto(), saved.getMonto(), saved.getCategoria(), saved.getMes());
    }

    public SaveGastoFijoUseCase(GastoFijoRepository gastoFijoRepository) {
        this.gastoFijoRepository = gastoFijoRepository;
    }
}
