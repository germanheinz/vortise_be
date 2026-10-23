/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeletePresupuestoLineaUseCase {
    private final PresupuestoLineaRepository presupuestoLineaRepository;

    @Transactional
    public void execute(Long proyectoId, Long id) {
        this.presupuestoLineaRepository.findById(id)
            .filter(linea -> linea.getProyecto() != null && proyectoId.equals(linea.getProyecto().getId()))
            .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado"));
        this.presupuestoLineaRepository.deleteById(id);
    }

    public DeletePresupuestoLineaUseCase(PresupuestoLineaRepository presupuestoLineaRepository) {
        this.presupuestoLineaRepository = presupuestoLineaRepository;
    }
}
