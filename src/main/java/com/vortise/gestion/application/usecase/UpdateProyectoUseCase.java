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
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
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
    private final RestriccionObraRepository restriccionRepository;

    public UpdateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository,
            com.vortise.gestion.domain.repository.ObraTareaRepository taskRepository,
            RestriccionObraRepository restriccionRepository,
            com.vortise.gestion.infrastructure.security.CurrentTenantService currentTenant) {
        this.taskRepository = taskRepository;
        this.currentTenant = currentTenant;
        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
        this.catalogoRepository = catalogoRepository;
        this.restriccionRepository = restriccionRepository;
    }

    @SuppressWarnings("null")
    public ProyectoDto execute(Long proyectoId, ProyectoCreateDto dto) {
        validateDates(dto);
        if (dto.empresaId() != null) currentTenant.requireEmpresa(dto.empresaId());
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Long empresaAnteriorId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        Long empresaDestinoId = dto.empresaId() == null ? empresaAnteriorId : dto.empresaId();
        if (!java.util.Objects.equals(empresaAnteriorId, empresaDestinoId) && hasContractorReferences(proyecto)) {
            throw new IllegalArgumentException("No se puede cambiar la empresa de una obra mientras tenga subcontratistas seleccionados o asignados");
        }

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
        List<Long> selectedContractorIds = dto.subcontratistaIds() == null
            ? existingContractorIds(proyecto) : dto.subcontratistaIds();
        proyecto.setSubcontratistas(resolveSelectedSubcontratistas(proyecto, selectedContractorIds));

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
        List<RubroObra> updated = dto.rubros() == null ? new ArrayList<>(proyecto.getRubros()) : mapRubros(dto.rubros(), proyecto);
        var retainedIds = updated.stream().map(RubroObra::getId).filter(java.util.Objects::nonNull).toList();
        if (linkedIds.stream().anyMatch(id -> !retainedIds.contains(id))) {
            throw new IllegalArgumentException("No se puede eliminar una actividad con tareas vinculadas");
        }
        for (RubroObra existing : proyecto.getRubros()) {
            if (existing.getId() == null || restriccionRepository.countByRubroVinculadoId(existing.getId()) == 0) continue;
            RubroCreateDto requested = dto.rubros() == null ? null : dto.rubros().stream()
                .filter(candidate -> existing.getId().equals(candidate.id())).findFirst().orElse(null);
            if (requested == null) {
                throw new IllegalArgumentException("No se puede eliminar una actividad vinculada a restricciones");
            }
            if (requested.subRubro() == null || requested.subRubro().isBlank()) {
                throw new IllegalArgumentException("No se puede convertir en rubro padre una actividad vinculada a restricciones");
            }
        }
        if (dto.rubros() != null) {
            proyecto.getRubros().removeIf(r -> !updated.contains(r));
            updated.stream().filter(r -> !proyecto.getRubros().contains(r)).forEach(proyecto.getRubros()::add);
        }
        validateSelectedContractors(updated, proyecto);
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

    @SuppressWarnings("null")
    private CatalogoEmpresa resolveSubcontratista(Proyecto proyecto, Long subcontratistaId) {
        if (subcontratistaId == null) {
            return null;
        }
        CatalogoEmpresa subcontratista = catalogoRepository.findById(subcontratistaId)
            .orElseThrow(() -> new IllegalArgumentException("Subcontratista no encontrado"));
        Long empresaProyecto = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        if (empresaProyecto == null || !subcontratista.getEmpresa().getId().equals(empresaProyecto)
            || !"SUBCONTRATISTA".equalsIgnoreCase(subcontratista.getTipo())) {
            throw new IllegalArgumentException("El subcontratista no pertenece a la empresa de la obra");
        }
        if (proyecto.getSubcontratistas() == null || proyecto.getSubcontratistas().stream()
                .noneMatch(selected -> subcontratistaId.equals(selected.getId()))) {
            throw new IllegalArgumentException("El subcontratista no está seleccionado para esta obra");
        }
        return subcontratista;
    }

    @SuppressWarnings("null")
    private List<Long> existingContractorIds(Proyecto proyecto) {
        java.util.Set<Long> ids = new java.util.TreeSet<>();
        if (proyecto.getSubcontratistas() != null) proyecto.getSubcontratistas().stream()
            .map(CatalogoEmpresa::getId).filter(java.util.Objects::nonNull).forEach(ids::add);
        if (proyecto.getRubros() != null) proyecto.getRubros().stream().map(RubroObra::getSubcontratista)
            .filter(java.util.Objects::nonNull).map(CatalogoEmpresa::getId).filter(java.util.Objects::nonNull).forEach(ids::add);
        return List.copyOf(ids);
    }

    private void validateSelectedContractors(List<RubroObra> rubros, Proyecto proyecto) {
        for (RubroObra rubro : rubros) {
            CatalogoEmpresa contractor = rubro.getSubcontratista();
            if (contractor == null) continue;
            boolean selected = proyecto.getSubcontratistas() != null && proyecto.getSubcontratistas().stream()
                .anyMatch(item -> item.getId().equals(contractor.getId()));
            if (!selected) throw new IllegalArgumentException("No se puede quitar un subcontratista que sigue asignado a un subrubro");
        }
    }

    private java.util.Set<CatalogoEmpresa> resolveSelectedSubcontratistas(Proyecto proyecto, List<Long> ids) {
        java.util.Set<Long> unique = new java.util.HashSet<>(ids);
        if (unique.size() != ids.size()) throw new IllegalArgumentException("Hay subcontratistas duplicados");
        Long companyId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        return unique.stream().map(id -> catalogoRepository.findById(id)
            .filter(item -> companyId != null && companyId.equals(item.getEmpresa().getId())
                && "SUBCONTRATISTA".equalsIgnoreCase(item.getTipo()))
            .orElseThrow(() -> new IllegalArgumentException("El subcontratista no pertenece a la empresa de la obra")))
            .collect(java.util.stream.Collectors.toSet());
    }

    @SuppressWarnings("null")
    private boolean hasContractorReferences(Proyecto proyecto) {
        return (proyecto.getSubcontratistas() != null && !proyecto.getSubcontratistas().isEmpty())
            || (proyecto.getRubros() != null && proyecto.getRubros().stream()
                .anyMatch(rubro -> rubro.getSubcontratista() != null));
    }
}