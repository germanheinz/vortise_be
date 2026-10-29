package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetEmpresasUseCase {
    private final EmpresaRepository empresaRepository;
    private final CurrentIdentity currentTenant;

    public GetEmpresasUseCase(EmpresaRepository empresaRepository, CurrentIdentity currentTenant) {
        this.empresaRepository = empresaRepository;
        this.currentTenant = currentTenant;
    }

    public List<EmpresaDto> execute() {
        if (currentTenant.isSuperAdmin()) {
            return empresaRepository.findAll().stream().map(EmpresaMapper::toDto).toList();
        }

        if (currentTenant.isCollaborator()) {
            return currentTenant.empresasAccesibles().stream().flatMap(id -> empresaRepository.findById(id).stream())
                .map(EmpresaMapper::toDto).toList();
        }
        Long ownId = currentTenant.empresaId();
        List<Empresa> empresas = new ArrayList<>(empresaRepository.findById(ownId).stream().toList());
        if (currentTenant.isAdministrator()) {
            empresaRepository.findByCreadoPorUsuarioId(currentTenant.usuarioId()).stream()
                .filter(empresa -> !empresa.getId().equals(ownId))
                .forEach(empresas::add);
        }
        return empresas.stream().map(EmpresaMapper::toDto).toList();
    }
}
