package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ContratoObraDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.ContratoObra;
import com.vortise.gestion.domain.model.Proyecto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Maps the contracts requested for a project; shared by the create and update use cases. */
final class ContratosObra {
    private ContratosObra() { }

    /**
     * Matches requested contracts to the stored ones by id and creates the rest. Must run after the project's contractors are
     * resolved. Returns the resulting contracts in request order; omitted stored contracts are not in the result.
     */
    static List<ContratoObra> aplicar(Proyecto proyecto, List<ContratoObraDto> dtos) {
        Map<Long, ContratoObra> existentes = new HashMap<>();
        if (proyecto.getContratos() != null) {
            for (ContratoObra contrato : proyecto.getContratos()) {
                if (contrato.getId() != null) existentes.put(contrato.getId(), contrato);
            }
        }
        Set<String> nombres = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        List<ContratoObra> result = new ArrayList<>();
        for (ContratoObraDto dto : dtos) {
            String nombre = dto.nombre() == null ? "" : dto.nombre().trim();
            if (nombre.isEmpty()) throw new SolicitudInvalidaException("El nombre del contrato es obligatorio");
            if (nombre.length() > 255) throw new SolicitudInvalidaException("El nombre del contrato no puede superar los 255 caracteres");
            if (!nombres.add(nombre.toLowerCase())) throw new SolicitudInvalidaException("Hay contratos con el mismo nombre");
            ContratoObra contrato;
            if (dto.id() == null) {
                contrato = new ContratoObra();
            } else {
                if (!ids.add(dto.id())) throw new SolicitudInvalidaException("Contrato duplicado");
                contrato = existentes.get(dto.id());
                if (contrato == null) throw new SolicitudInvalidaException("El contrato no pertenece a la obra");
            }
            contrato.setProyecto(proyecto);
            contrato.setNombre(nombre);
            contrato.setSubcontratista(contratista(proyecto, dto));
            result.add(contrato);
        }
        return result;
    }

    static Map<Long, ContratoObra> porId(List<ContratoObra> contratos) {
        Map<Long, ContratoObra> result = new HashMap<>();
        if (contratos != null) {
            for (ContratoObra contrato : contratos) {
                if (contrato.getId() != null) result.put(contrato.getId(), contrato);
            }
        }
        return result;
    }

    /** Own labour (null) unless the request names a contractor already selected for the project, by id or by name. */
    private static CatalogoEmpresa contratista(Proyecto proyecto, ContratoObraDto dto) {
        Set<CatalogoEmpresa> seleccionados = proyecto.getSubcontratistas() == null ? Set.of() : proyecto.getSubcontratistas();
        if (dto.subcontratistaId() != null) {
            return seleccionados.stream().filter(item -> dto.subcontratistaId().equals(item.getId())).findFirst()
                .orElseThrow(() -> new SolicitudInvalidaException("El contratista del contrato no está seleccionado en la obra"));
        }
        String nombre = dto.subcontratistaNombre() == null ? "" : dto.subcontratistaNombre().trim();
        if (nombre.isEmpty()) return null;
        return seleccionados.stream().filter(item -> nombre.equalsIgnoreCase(item.getNombre())).findFirst()
            .orElseThrow(() -> new SolicitudInvalidaException("El contratista del contrato no está seleccionado en la obra"));
    }
}
