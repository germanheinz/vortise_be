/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.repository.RendimientoRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteRendimientoUseCase {
    private final RendimientoRepository rendimientoRepository;

    public void execute(Long id) {
        this.rendimientoRepository.deleteById(id);
    }

    public DeleteRendimientoUseCase(RendimientoRepository rendimientoRepository) {
        this.rendimientoRepository = rendimientoRepository;
    }
}
