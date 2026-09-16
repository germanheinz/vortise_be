package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;

@Service
public class GetEmpresasUseCase {
    private final EmpresaRepository empresaRepository;
    private final CurrentTenantService currentTenant;

    public GetEmpresasUseCase(EmpresaRepository empresaRepository, CurrentTenantService currentTenant) {
        this.empresaRepository = empresaRepository;
        this.currentTenant = currentTenant;
    }

    public List<EmpresaDto> execute() {
        if (currentTenant.isAdministrator()) {
            return empresaRepository.findAll().stream().map(EmpresaMapper::toDto).toList();
        }

        return empresaRepository.findById(currentTenant.empresaId()).stream().map(EmpresaMapper::toDto).toList();
    }
}
