/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.repository.GastoFijoRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteGastoFijoUseCase {
    private final GastoFijoRepository gastoFijoRepository;

    public void execute(Long id) {
        this.gastoFijoRepository.deleteById(id);
    }

    public DeleteGastoFijoUseCase(GastoFijoRepository gastoFijoRepository) {
        this.gastoFijoRepository = gastoFijoRepository;
    }
}
