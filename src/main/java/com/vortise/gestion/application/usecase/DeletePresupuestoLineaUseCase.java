/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import org.springframework.stereotype.Service;

@Service
public class DeletePresupuestoLineaUseCase {
    private final PresupuestoLineaRepository presupuestoLineaRepository;

    public void execute(Long id) {
        this.presupuestoLineaRepository.deleteById(id);
    }

    public DeletePresupuestoLineaUseCase(PresupuestoLineaRepository presupuestoLineaRepository) {
        this.presupuestoLineaRepository = presupuestoLineaRepository;
    }
}
