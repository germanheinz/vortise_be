package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaCreateDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
@org.springframework.transaction.annotation.Transactional
public class UpdateProyectoUseCase {
    private final com.vortise.gestion.domain.repository.ObraTareaRepository taskRepository;
    private final com.vortise.gestion.infrastructure.security.CurrentTenantService currentTenant;
    private final ProyectoRepository proyectoRepository;
    private final EmpresaRepository empresaRepository;
    private final CatalogoEmpresaRepository catalogoRepository;

    public UpdateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository, com.vortise.gestion.domain.repository.ObraTareaRepository taskRepository, com.vortise.gestion.infrastructure.security.CurrentTenantService currentTenant) {
        this.taskRepository = taskRepository;
        this.currentTenant = currentTenant;
        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
        this.catalogoRepository = catalogoRepository;
    }

    public ProyectoDto execute(Long proyectoId, ProyectoCreateDto dto) {
        validateDates(dto);
        if (dto.empresaId() != null) currentTenant.requireEmpresa(dto.empresaId());
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        proyecto.setNombre(dto.nombre());
        proyecto.setDescripcion(dto.descripcion());
        proyecto.setPresupuestoUsd(dto.presupuestoUsd());
        proyecto.setDireccion(dto.direccion());
        proyecto.setNumeroProyecto(dto.numeroProyecto());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setFechaFin(dto.fechaFin());
        proyecto.setResponsablesObra(dto.responsablesObra());
        proyecto.setCategoriasManoObra(dto.categoriasManoObra());
        proyecto.setCantidadSectores(dto.cantidadSectores());
        proyecto.setEmpresa(dto.empresa());
        if (dto.empresaId() != null) {
            proyecto.setEmpresaRelacionada(empresaRepository.findById(dto.empresaId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada")));
        }
        proyecto.setHorasPrevistas(dto.horasPrevistas());
        proyecto.setHorasReales(dto.horasReales());

        // Keep managed collection references to satisfy orphanRemoval semantics.
        if (proyecto.getPlantas() == null) {
            proyecto.setPlantas(new ArrayList<>());
        }
        proyecto.getPlantas().clear();
        proyecto.getPlantas().addAll(mapPlantas(dto.plantas(), proyecto));

        if (proyecto.getRubros() == null) {
            proyecto.setRubros(new ArrayList<>());
        }
        var linkedIds = taskRepository.findByProyectoId(proyectoId).stream().map(t -> t.getRubroId())
            .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (dto.rubros() != null) {
            for (var requested : dto.rubros()) {
                if (requested.id() == null || !linkedIds.contains(requested.id())) continue;
                var existing = proyecto.getRubros().stream().filter(r -> requested.id().equals(r.getId())).findFirst().orElseThrow();
                if (!java.util.Objects.equals(existing.getUnidad(), requested.unidad())) {
                    throw new IllegalArgumentException("No se puede cambiar la unidad de una actividad con tareas vinculadas");
                }
            }
        }
        List<RubroObra> updated = mapRubros(dto.rubros(), proyecto);
        var retainedIds = updated.stream().map(RubroObra::getId).filter(java.util.Objects::nonNull).toList();
        if (linkedIds.stream().anyMatch(id -> !retainedIds.contains(id))) {
            throw new IllegalArgumentException("No se puede eliminar una actividad con tareas vinculadas");
        }
        proyecto.getRubros().removeIf(r -> !updated.contains(r));
        updated.stream().filter(r -> !proyecto.getRubros().contains(r)).forEach(proyecto.getRubros()::add);
        if (!updated.isEmpty()) proyecto.setHorasPrevistas(updated.stream().mapToDouble(r ->
            r.getHorasOficialesPrevistas() != null && r.getHorasAyudantesPrevistas() != null
                ? r.getHorasOficialesPrevistas() + r.getHorasAyudantesPrevistas()
                : (r.getCantidad() == null ? 0 : r.getCantidad()) * (r.getProductividad() == null ? 0 : r.getProductividad())).sum());

        Proyecto saved = proyectoRepository.save(proyecto);
        return ProyectoMapper.toDetailDto(saved);
    }

    private void validateDates(ProyectoCreateDto dto) {
        if (dto.fechaInicio() != null && dto.fechaFin() != null && dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior al inicio de la obra");
        }
    }

    private List<Planta> mapPlantas(List<PlantaCreateDto> plantas, Proyecto proyecto) {
        if (plantas == null) {
            return new ArrayList<>();
        }

        List<Planta> result = new ArrayList<>();
        for (PlantaCreateDto plantaDto : plantas) {
            Planta planta = new Planta();
            planta.setProyecto(proyecto);
            planta.setNivel(plantaDto.nivel());
            planta.setSuperficie(plantaDto.superficie());
            planta.setSupCubierta(plantaDto.supCubierta());
            planta.setSupDescubierta(plantaDto.supDescubierta());
            planta.setSupCalculada(plantaDto.supCalculada());
            planta.setCoefPlantaTipo(plantaDto.coefPlantaTipo());
            result.add(planta);
        }
        return result;
    }

    private List<RubroObra> mapRubros(List<RubroCreateDto> rubros, Proyecto proyecto) {
        if (rubros == null) {
            return new ArrayList<>();
        }

        List<RubroObra> result = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (RubroCreateDto rubroDto : rubros) {
            if (rubroDto.id() != null && !seen.add(rubroDto.id())) throw new IllegalArgumentException("Actividad duplicada");
            RubroObra rubro = rubroDto.id() == null ? new RubroObra() : proyecto.getRubros().stream()
                .filter(r -> rubroDto.id().equals(r.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La actividad no pertenece a la obra"));
            rubro.setProyecto(proyecto);
            rubro.setnRubro(rubroDto.nRubro());
            rubro.setRubro(rubroDto.rubro());
            rubro.setNivel(rubroDto.nivel());
            rubro.setSubNumeroRubro(rubroDto.subNumeroRubro());
            rubro.setSubRubro(rubroDto.subRubro());
            rubro.setCantidad(rubroDto.cantidad());
            rubro.setUnidad(rubroDto.unidad());
            rubro.setProductividad(rubroDto.productividad());
            rubro.setEmpresa(rubroDto.empresa());
            rubro.setTipoContratista(rubroDto.tipoContratista() == null ? "PROPIA" : rubroDto.tipoContratista());
            rubro.setSubcontratista(resolveSubcontratista(proyecto, rubroDto.subcontratistaId()));
            rubro.setHorasOficialesPrevistas(rubroDto.horasOficialesPrevistas());
            rubro.setHorasAyudantesPrevistas(rubroDto.horasAyudantesPrevistas());
            rubro.setCantidadPersonas(rubroDto.cantidadPersonas());
            rubro.setFechaInicioPlanificada(rubroDto.fechaInicioPlanificada());
            rubro.setFechaFinPlanificada(rubroDto.fechaFinPlanificada());
            rubro.setCronogramaManual(rubroDto.cronogramaManual());
            RubroPlanning.apply(rubro, rubroDto);
            result.add(rubro);
        }
        return result;
    }

    private CatalogoEmpresa resolveSubcontratista(Proyecto proyecto, Long subcontratistaId) {
        if (subcontratistaId == null) {
            return null;
        }
        CatalogoEmpresa subcontratista = catalogoRepository.findById(subcontratistaId)
            .orElseThrow(() -> new IllegalArgumentException("Subcontratista no encontrado"));
        Long empresaProyecto = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        if (empresaProyecto == null || !subcontratista.getEmpresa().getId().equals(empresaProyecto)) {
            throw new IllegalArgumentException("El subcontratista no pertenece a la empresa de la obra");
        }
        return subcontratista;
    }
}