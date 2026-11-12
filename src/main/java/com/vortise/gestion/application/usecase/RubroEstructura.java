package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.service.EstructuraRubros;
import com.vortise.gestion.domain.service.EstructuraRubros.Nodo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maps the requested activity structure onto {@link RubroObra} rows; shared by the create and update use cases. */
final class RubroEstructura {
    private RubroEstructura() { }

    /**
     * Validates the requested tree. {@code existentes} is parallel to {@code dtos} and holds the stored row matched by
     * id (null for new rows); it may be null when every row is new.
     */
    static List<Nodo> resolver(List<RubroCreateDto> dtos, RubroObra[] existentes) {
        List<EstructuraRubros.Entrada> entradas = new ArrayList<>(dtos.size());
        for (int i = 0; i < dtos.size(); i++) {
            RubroCreateDto dto = dtos.get(i);
            RubroObra existente = existentes == null ? null : existentes[i];
            entradas.add(new EstructuraRubros.Entrada(dto.nRubro(), dto.parentCodigo(), dto.tipoNodo(),
                dto.varianteEstructura(), dto.nombre(), dto.rubro(), dto.subRubro(), dto.nivel(),
                existente == null ? null : existente.getTipoNodo(),
                existente == null ? null : existente.getVarianteEstructura()));
        }
        return EstructuraRubros.resolver(entradas);
    }

    /** Identity and structure columns, valid for every node. */
    static void aplicarEstructura(RubroObra rubro, RubroCreateDto dto, Nodo nodo) {
        rubro.setnRubro(nodo.codigo() != null ? nodo.codigo() : dto.nRubro());
        rubro.setTipoNodo(nodo.tipo());
        rubro.setVarianteEstructura(nodo.variante());
        rubro.setNombre(nodo.nombre());
        rubro.setRubro(nodo.rubro());
        rubro.setSubRubro(nodo.subRubro());
        rubro.setNivel(nodo.nivel());
        rubro.setContrato(nodo.tipo() == TipoNodoRubro.RUBRO ? contratoDe(dto.contrato()) : null);
    }

    private static String contratoDe(String contrato) {
        String value = contrato == null ? "" : contrato.trim();
        if (value.length() > 255) throw new SolicitudInvalidaException("El contrato no puede superar los 255 caracteres");
        return value.isEmpty() ? null : value;
    }

    /** Activity data: only leaves carry it. */
    static void aplicarHoja(RubroObra rubro, RubroCreateDto dto, CatalogoEmpresa subcontratista) {
        rubro.setSubNumeroRubro(dto.subNumeroRubro());
        rubro.setCantidad(dto.cantidad());
        rubro.setUnidad(dto.unidad());
        rubro.setProductividad(dto.productividad());
        rubro.setEmpresa(dto.empresa());
        rubro.setTipoContratista(dto.tipoContratista() == null ? "PROPIA" : dto.tipoContratista());
        rubro.setSubcontratista(subcontratista);
        rubro.setHorasOficialesPrevistas(dto.horasOficialesPrevistas());
        rubro.setHorasAyudantesPrevistas(dto.horasAyudantesPrevistas());
        rubro.setCantidadPersonas(dto.cantidadPersonas());
        rubro.setFechaInicioPlanificada(dto.fechaInicioPlanificada());
        rubro.setFechaFinPlanificada(dto.fechaFinPlanificada());
        rubro.setCronogramaManual(dto.cronogramaManual());
        RubroPlanning.apply(rubro, dto);
    }

    /** Grouping nodes (root rubro and intermediate nodes) carry no activity data. */
    static void limpiarAgrupador(RubroObra rubro) {
        rubro.setSubNumeroRubro(null);
        rubro.setCantidad(null);
        rubro.setUnidad(null);
        rubro.setProductividad(null);
        rubro.setEmpresa(null);
        rubro.setTipoContratista("PROPIA");
        rubro.setSubcontratista(null);
        rubro.setHorasOficialesPrevistas(null);
        rubro.setHorasAyudantesPrevistas(null);
        rubro.setRendimientoOficial(null);
        rubro.setRendimientoAyudante(null);
        rubro.setCantidadPersonas(null);
        rubro.setFechaInicioPlanificada(null);
        rubro.setFechaFinPlanificada(null);
        rubro.setCronogramaManual(false);
        rubro.setFrentes(1);
    }

    /** Links each row to its parent row (resolved by code inside the same request). {@code rubros} is parallel to {@code nodos}. */
    static void enlazarPadres(List<RubroObra> rubros, List<Nodo> nodos) {
        Map<String, RubroObra> porCodigo = new HashMap<>();
        for (int i = 0; i < nodos.size(); i++) {
            if (nodos.get(i).codigo() != null) porCodigo.put(nodos.get(i).codigo(), rubros.get(i));
        }
        for (int i = 0; i < nodos.size(); i++) {
            String parentCodigo = nodos.get(i).parentCodigo();
            rubros.get(i).setParent(parentCodigo == null ? null : porCodigo.get(parentCodigo));
        }
    }

    /** Rows ordered so that parents come before their children (stable inside the same layer). */
    static List<RubroObra> padresPrimero(List<RubroObra> rubros, List<Nodo> nodos) {
        List<Integer> orden = new ArrayList<>(nodos.size());
        for (int i = 0; i < nodos.size(); i++) orden.add(i);
        orden.sort(Comparator.comparingInt(i -> nodos.get(i).capa()));
        List<RubroObra> result = new ArrayList<>(rubros.size());
        for (int i : orden) result.add(rubros.get(i));
        return result;
    }
}
