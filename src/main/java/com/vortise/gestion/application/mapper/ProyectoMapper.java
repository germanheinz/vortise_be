package com.vortise.gestion.application.mapper;

import com.vortise.gestion.application.dto.CalendarioObraDto;
import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.application.dto.ContratoObraDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroDto;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.ContratoObra;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.rule.EstructuraRubros;
import java.util.List;

public final class ProyectoMapper {
    private ProyectoMapper() {
    }

    public static ProyectoDto toSummaryDto(Proyecto proyecto) {
        return toDto(proyecto, false);
    }

    public static ProyectoDto toDetailDto(Proyecto proyecto) {
        return toDto(proyecto, true);
    }

    private static ProyectoDto toDto(Proyecto proyecto, boolean incluirDetalle) {
        return new ProyectoDto(
            proyecto.getId(),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            proyecto.getPresupuestoUsd(),
            proyecto.getDireccion(),
            proyecto.getNumeroProyecto(),
            proyecto.getFechaInicio(),
            proyecto.getFechaFin(),
            proyecto.getResponsablesObra(),
            proyecto.getCategoriasManoObra(),
            proyecto.getCantidadSectores(),
            proyecto.getEmpresa(),
            proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId(),
            proyecto.getEmpresaRelacionada() == null ? proyecto.getEmpresa() : proyecto.getEmpresaRelacionada().getNombre(),
            proyecto.getHorasPrevistas(),
            proyecto.getHorasReales(),
            incluirDetalle ? plantasDe(proyecto) : null,
            incluirDetalle ? rubrosDe(proyecto) : null,
            incluirDetalle ? selectedContractorIds(proyecto) : null,
            proyecto.getSuperficieCubierta(),
            proyecto.getSuperficieSemicubierta(),
            proyecto.getSuperficieDescubierta(),
            proyecto.getPedidosDeObraDias(),
            incluirDetalle ? contratosDe(proyecto) : null,
            calendarioDe(proyecto)
        );
    }

    private static CalendarioObraDto calendarioDe(Proyecto proyecto) {
        return new CalendarioObraDto(proyecto.isTrabajaSabado(), proyecto.isTrabajaDomingo(),
            proyecto.isPagaDobleFeriado(), proyecto.isPagaDobleNoLaborable());
    }

    private static List<PlantaDto> plantasDe(Proyecto proyecto) {
        var plantas = proyecto.getPlantas();
        return plantas == null ? List.of() : plantas.stream().map(ProyectoMapper::toPlantaDto).toList();
    }

    private static List<RubroDto> rubrosDe(Proyecto proyecto) {
        var rubros = proyecto.getRubros();
        return rubros == null ? List.of() : rubros.stream()
            .sorted(EstructuraRubros.ORDEN_NATURAL)
            .map(rubro -> toRubroDto(rubro, rubros)).toList();
    }

    private static List<ContratoObraDto> contratosDe(Proyecto proyecto) {
        var contratos = proyecto.getContratos();
        return contratos == null ? List.of() : contratos.stream().map(ProyectoMapper::toContratoDto).toList();
    }

    private static ContratoObraDto toContratoDto(ContratoObra contrato) {
        return new ContratoObraDto(contrato.getId(), contrato.getNombre(),
            contrato.getSubcontratista() == null ? null : contrato.getSubcontratista().getId(),
            contrato.getSubcontratista() == null ? null : contrato.getSubcontratista().getNombre());
    }

    @SuppressWarnings("null")
    private static List<Long> selectedContractorIds(Proyecto proyecto) {
        java.util.Set<Long> ids = new java.util.TreeSet<>();
        if (proyecto.getSubcontratistas() != null) proyecto.getSubcontratistas().stream()
            .map(item -> item.getId()).filter(java.util.Objects::nonNull).forEach(ids::add);
        if (proyecto.getRubros() != null) proyecto.getRubros().stream().map(RubroObra::getSubcontratista)
            .filter(java.util.Objects::nonNull).map(item -> item.getId()).filter(java.util.Objects::nonNull).forEach(ids::add);
        return List.copyOf(ids);
    }

    private static PlantaDto toPlantaDto(Planta planta) {
        return new PlantaDto(
            planta.getId(),
            planta.getProyecto().getId(),
            planta.getNivel(),
            planta.getSuperficie(),
            planta.getSupCubierta(),
            planta.getSupDescubierta(),
            planta.getSupCalculada(),
            planta.getCoefPlantaTipo()
        );
    }

    private static RubroDto toRubroDto(RubroObra rubro, java.util.Collection<RubroObra> todos) {
        return new RubroDto(
            rubro.getId(),
            rubro.getProyecto().getId(),
            rubro.getnRubro(),
            rubro.getRubro(),
            rubro.getNivel(),
            rubro.getSubNumeroRubro(),
            rubro.getSubRubro(),
            rubro.getCantidad(),
            rubro.getUnidad(),
            rubro.getProductividad(),
            rubro.getEmpresa(),
            rubro.getTipoContratista(),
            rubro.getSubcontratista() == null ? null : rubro.getSubcontratista().getId(),
            rubro.getSubcontratista() == null ? null : rubro.getSubcontratista().getNombre(),
            rubro.getHorasOficialesPrevistas(),
            rubro.getHorasAyudantesPrevistas(),
            rubro.getRendimientoOficial(),
            rubro.getRendimientoAyudante(),
            rubro.getCantidadPersonas(),
            rubro.getFechaInicioPlanificada(),
            rubro.getFechaFinPlanificada(),
            rubro.isCronogramaManual(),
            rubro.getFrentes(),
            rubro.getParent() == null ? null : rubro.getParent().getId(),
            rubro.getTipoNodo().name(),
            rubro.getVarianteEstructura() == null ? null : rubro.getVarianteEstructura().name(),
            EstructuraRubros.nombreDe(rubro),
            EstructuraRubros.esHoja(rubro, todos),
            rubro.getContrato() == null ? null : rubro.getContrato().getId(),
            rubro.getContrato() == null ? null : rubro.getContrato().getNombre()
        );
    }
}