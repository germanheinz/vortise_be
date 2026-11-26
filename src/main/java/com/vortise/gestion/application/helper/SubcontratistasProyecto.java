package com.vortise.gestion.application.helper;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SubcontratistasProyecto {
    private final CatalogoEmpresaRepository catalogoRepository;

    public SubcontratistasProyecto(CatalogoEmpresaRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    public Set<CatalogoEmpresa> resolverSeleccion(Proyecto proyecto, List<Long> ids) {
        Set<Long> idsUnicos = new HashSet<>(ids);
        if (idsUnicos.size() != ids.size()) throw new SolicitudInvalidaException("Hay subcontratistas duplicados");
        Long empresaId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        return idsUnicos.stream().map(id -> catalogoRepository.findById(id)
            .filter(item -> empresaId != null && empresaId.equals(item.getEmpresa().getId())
                && "SUBCONTRATISTA".equalsIgnoreCase(item.getTipo()))
            .orElseThrow(() -> new SolicitudInvalidaException("El subcontratista no pertenece a la empresa de la obra")))
            .collect(Collectors.toSet());
    }

    public CatalogoEmpresa resolverAsignado(Proyecto proyecto, Long subcontratistaId) {
        if (subcontratistaId == null) {
            return null;
        }
        CatalogoEmpresa subcontratista = catalogoRepository.findById(subcontratistaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Subcontratista no encontrado"));
        Long empresaProyecto = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        if (empresaProyecto == null || !subcontratista.getEmpresa().getId().equals(empresaProyecto)
            || !"SUBCONTRATISTA".equalsIgnoreCase(subcontratista.getTipo())) {
            throw new SolicitudInvalidaException("El subcontratista no pertenece a la empresa de la obra");
        }
        if (proyecto.getSubcontratistas() == null || proyecto.getSubcontratistas().stream()
                .noneMatch(selected -> subcontratistaId.equals(selected.getId()))) {
            throw new SolicitudInvalidaException("El subcontratista no está seleccionado para esta obra");
        }
        return subcontratista;
    }
}
