package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveEmpresaUseCase {
    private final EmpresaRepository empresaRepository;
    private final CurrentIdentity currentTenant;

    private final InicializarEmpresaUseCase inicializar;

    public SaveEmpresaUseCase(EmpresaRepository empresaRepository, CurrentIdentity currentTenant, InicializarEmpresaUseCase inicializar) {
        this.empresaRepository = empresaRepository;
        this.currentTenant = currentTenant;
        this.inicializar = inicializar;
    }

    @Transactional
    public EmpresaDto create(EmpresaCreateDto dto) {
        ensureNameAvailable(dto.nombre(), null);
        ensureTaxIdAvailable(dto.identificacionFiscal(), null);
        Empresa empresa = new Empresa(dto.nombre().trim(), normalize(dto.identificacionFiscal()));
        empresa.setCreadoPorUsuarioId(currentTenant.usuarioId());
        Empresa saved = empresaRepository.save(empresa);
        inicializar.execute(saved.getId());
        return EmpresaMapper.toDto(saved);
    }

    @Transactional
    public EmpresaDto update(Long id, EmpresaCreateDto dto) {
        Empresa empresa = empresaRepository.bloquearPorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        currentTenant.requireEmpresa(id);
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
                throw new SolicitudInvalidaException("Ya existe una empresa con ese nombre");
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
                throw new ConflictoEstadoException("Ya existe una empresa con esa identificación fiscal");
            });
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
