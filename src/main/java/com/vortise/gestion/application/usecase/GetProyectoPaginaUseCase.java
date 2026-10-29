package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.application.exception.*;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProyectoPaginaUseCase {
    private final ProyectoRepository projects;
    private final EmpresaRepository companies;
    private final CurrentIdentity identity;
    public GetProyectoPaginaUseCase(ProyectoRepository projects, EmpresaRepository companies, CurrentIdentity identity) {
        this.projects = projects; this.companies = companies; this.identity = identity;
    }
    @Transactional(readOnly = true)
    public PaginaDto<ProyectoDto> execute(Long empresaId, String q, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || q == null || q.length() > 160) {
            throw new SolicitudInvalidaException("Parámetros de paginación inválidos");
        }
        Long companyId = empresaId == null ? identity.empresaId() : empresaId;
        identity.requireEmpresa(companyId);
        if (companies.findById(companyId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
        var result = projects.buscarPagina(companyId, q.trim(), page, size);
        return new PaginaDto<>(result.content().stream().map(ProyectoMapper::toSummaryDto).toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
