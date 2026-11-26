package com.vortise.gestion.service;

import com.vortise.gestion.application.mapper.ProyectoMapper;
import com.vortise.gestion.application.dto.PaginaDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyectoService {
    private final ProyectoRepository projects;
    private final EmpresaRepository companies;
    private final CurrentIdentity identity;
    public ProyectoService(ProyectoRepository projects, EmpresaRepository companies, CurrentIdentity identity) {
        this.projects = projects;
        this.companies = companies;
        this.identity = identity;
    }
    @Transactional(readOnly = true)
    public PaginaDto<ProyectoDto> paginar(Long empresaId, String q, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || q == null || q.length() > 160) {
            throw new SolicitudInvalidaException("Parámetros de paginación inválidos");
        }
        Long companyId = empresaId == null ? identity.empresaId() : empresaId;
        identity.requireEmpresa(companyId);
        if (companies.findById(companyId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
        var result = projects.buscarPagina(companyId, q.trim(), page, size);
        return new PaginaDto<>(result.content().stream().map(ProyectoMapper::toDetailDto).toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public List<ProyectoDto> listarDeEmpresaActual() {
        return projects.findByEmpresaRelacionadaId(identity.empresaId()).stream()
            .map(ProyectoMapper::toDetailDto).toList();
    }

    public ProyectoDto obtener(Long proyectoId) {
        var proyecto = projects.findById(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        return ProyectoMapper.toDetailDto(proyecto);
    }

    public List<ProyectoDto> listarResumenesPorEmpresa(Long empresaId) {
        if (companies.findById(empresaId).isEmpty()) {
            throw new RecursoNoEncontradoException("Empresa no encontrada");
        }
        return projects.findByEmpresaRelacionadaId(empresaId).stream()
            .map(ProyectoMapper::toSummaryDto)
            .toList();
    }

    public void eliminar(Long proyectoId) {
        if (projects.findById(proyectoId).isEmpty()) {
            throw new RecursoNoEncontradoException("Proyecto no encontrado");
        }

        projects.deleteById(proyectoId);
    }
}
