package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import org.springframework.stereotype.Service;

@Service
public class SaveEmpresaUseCase {
    private final EmpresaRepository empresaRepository;
    private final CurrentTenantService currentTenant;

    public SaveEmpresaUseCase(EmpresaRepository empresaRepository, CurrentTenantService currentTenant) {
        this.empresaRepository = empresaRepository;
        this.currentTenant = currentTenant;
    }

    public EmpresaDto create(EmpresaCreateDto dto) {
        ensureNameAvailable(dto.nombre(), null);
        ensureTaxIdAvailable(dto.identificacionFiscal(), null);
        Empresa empresa = new Empresa(dto.nombre().trim(), normalize(dto.identificacionFiscal()));
        empresa.setCreadoPorUsuarioId(currentTenant.usuarioId());
        return EmpresaMapper.toDto(empresaRepository.save(empresa));
    }

    public EmpresaDto update(Long id, EmpresaCreateDto dto) {
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
        ensureNameAvailable(dto.nombre(), id);
        ensureTaxIdAvailable(dto.identificacionFiscal(), id);
        empresa.setNombre(dto.nombre().trim());
        empresa.setIdentificacionFiscal(normalize(dto.identificacionFiscal()));
        return EmpresaMapper.toDto(empresaRepository.save(empresa));
    }

    private void ensureNameAvailable(String nombre, Long currentId) {
        empresaRepository.findByNombreIgnoreCase(nombre.trim())
            .filter(existing -> !existing.getId().equals(currentId))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Ya existe una empresa con ese nombre");
            });
    }

    private void ensureTaxIdAvailable(String identificacionFiscal, Long currentId) {
        String taxId = normalize(identificacionFiscal);
        if (taxId == null) {
            return;
        }

        empresaRepository.findByIdentificacionFiscal(taxId)
            .filter(existing -> !existing.getId().equals(currentId))
            .ifPresent(existing -> {
                throw new IllegalStateException("Ya existe una empresa con esa identificación fiscal");
            });
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
