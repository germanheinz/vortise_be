package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroDto;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import java.util.List;

final class ProyectoMapper {
    private ProyectoMapper() {
    }

    static ProyectoDto toSummaryDto(Proyecto proyecto) {
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
            null,
            null,
            null,
            proyecto.getSuperficieCubierta(),
            proyecto.getSuperficieSemicubierta(),
            proyecto.getSuperficieDescubierta()
        );
    }

    static ProyectoDto toDetailDto(Proyecto proyecto) {
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
            proyecto.getPlantas() == null ? List.of() : proyecto.getPlantas().stream().map(ProyectoMapper::toPlantaDto).toList(),
            proyecto.getRubros() == null ? List.of() : proyecto.getRubros().stream().map(ProyectoMapper::toRubroDto).toList(),
            selectedContractorIds(proyecto),
            proyecto.getSuperficieCubierta(),
            proyecto.getSuperficieSemicubierta(),
            proyecto.getSuperficieDescubierta()
        );
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

    private static RubroDto toRubroDto(RubroObra rubro) {
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
            rubro.getFrentes()
        );
    }
}