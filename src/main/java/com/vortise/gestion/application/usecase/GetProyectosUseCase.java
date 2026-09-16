/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;

@Service
public class GetProyectosUseCase {
    private final ProyectoRepository proyectoRepository;
    private final CurrentTenantService currentTenant;

    public List<ProyectoDto> execute() {
        return this.proyectoRepository.findByEmpresaRelacionadaId(currentTenant.empresaId()).stream().map(ProyectoMapper::toSummaryDto).toList();
    }

    public GetProyectosUseCase(ProyectoRepository proyectoRepository, CurrentTenantService currentTenant) {
        this.proyectoRepository = proyectoRepository;
        this.currentTenant = currentTenant;
    }
}
