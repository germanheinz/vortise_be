package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

@Service
public class SaveEmpresaUseCase {
    private final EmpresaRepository empresaRepository;

    public SaveEmpresaUseCase(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public EmpresaDto create(EmpresaCreateDto dto) {
        ensureNameAvailable(dto.nombre(), null);
        ensureTaxIdAvailable(dto.identificacionFiscal(), null);
        return EmpresaMapper.toDto(empresaRepository.save(new Empresa(
            dto.nombre().trim(),
            normalize(dto.identificacionFiscal())
        )));
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
