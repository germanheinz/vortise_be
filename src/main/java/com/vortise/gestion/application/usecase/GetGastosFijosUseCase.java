/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.GastoFijoDto;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetGastosFijosUseCase {
    private final GastoFijoRepository gastoFijoRepository;

    public List<GastoFijoDto> execute() {
        return this.gastoFijoRepository.findAll().stream().map(g -> new GastoFijoDto(g.getId(), g.getConcepto(), g.getMonto(), g.getCategoria(), g.getMes())).toList();
    }

    public GetGastosFijosUseCase(GastoFijoRepository gastoFijoRepository) {
        this.gastoFijoRepository = gastoFijoRepository;
    }
}
