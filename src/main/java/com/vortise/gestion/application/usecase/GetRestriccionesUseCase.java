package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;

@Service
public class GetRestriccionesUseCase {
    private final RestriccionObraRepository restriccionRepository;
    private final CurrentTenantService currentTenant;

    public GetRestriccionesUseCase(RestriccionObraRepository restriccionRepository, CurrentTenantService currentTenant) {
        this.restriccionRepository = restriccionRepository;
        this.currentTenant = currentTenant;
    }

    public List<RestriccionDto> execute(Long proyectoId) {
        return restriccionRepository.findByProyectoId(proyectoId).stream().map(RestriccionMapper::toDto).toList();
    }

    public List<RestriccionDto> executeAll() {
        Long empresaId = currentTenant.empresaId();
        return restriccionRepository.findAll().stream()
            .filter(item -> item.getProyecto() != null && item.getProyecto().getEmpresaRelacionada() != null)
            .filter(item -> empresaId.equals(item.getProyecto().getEmpresaRelacionada().getId()))
            .map(RestriccionMapper::toDto)
            .toList();
    }
}