package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.ProyectoUpdateDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.ContratoObra;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.service.EstructuraRubros;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateProyectoUseCase {
    private final ObraTareaRepository taskRepository;
    private final CurrentIdentity currentTenant;
    private final ProyectoRepository proyectoRepository;
    private final EmpresaRepository empresaRepository;
    private final CatalogoEmpresaRepository catalogoRepository;
    private final RestriccionObraRepository restriccionRepository;
    private final RegistroHorasRepository horas;
    private final AplicarCambiosEmpresaObraUseCase cambiosEmpresa;
    private final CertificadoBloqueos bloqueosCertificados;

    @Autowired
    public UpdateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository,
            ObraTareaRepository taskRepository,
            RestriccionObraRepository restriccionRepository,
            CurrentIdentity currentTenant,
            RegistroHorasRepository horas,
            AplicarCambiosEmpresaObraUseCase cambiosEmpresa,
            CertificadoBloqueos bloqueosCertificados) {
        this.horas = horas;
        this.cambiosEmpresa = cambiosEmpresa;
        this.bloqueosCertificados = bloqueosCertificados;
        this.taskRepository = taskRepository;
        this.currentTenant = currentTenant;
        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
        this.catalogoRepository = catalogoRepository;
        this.restriccionRepository = restriccionRepository;
    }

    public UpdateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository,
            ObraTareaRepository taskRepository, RestriccionObraRepository restriccionRepository,
            CurrentIdentity currentTenant, RegistroHorasRepository horas) {
        this(proyectoRepository, empresaRepository, catalogoRepository, taskRepository, restriccionRepository, currentTenant, horas, null, null);
    }

    @SuppressWarnings("null")
    public ProyectoDto execute(Long proyectoId, ProyectoUpdateDto dto) {
        validateDates(dto);
        validateSurfaces(dto.superficieCubierta(), dto.superficieSemicubierta(), dto.superficieDescubierta());
        if (dto.actualizarPedidosDeObraDias() && dto.pedidosDeObraDias() != null && dto.pedidosDeObraDias() < 0) {
            throw new SolicitudInvalidaException("Pedidos de obra debe ser un entero no negativo");
        }
        if (dto.empresaId() != null) currentTenant.requireEmpresa(dto.empresaId());
        Proyecto proyecto = proyectoRepository.bloquearPorId(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        if (proyecto.getEmpresaRelacionada() == null || proyecto.getEmpresaRelacionada().getId() == null) {
            throw new AccessDeniedException("Recurso sin empresa asignada");
        }
        currentTenant.requireEmpresa(proyecto.getEmpresaRelacionada().getId());
        Long empresaAnteriorId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        Long empresaDestinoId = dto.empresaId() == null ? empresaAnteriorId : dto.empresaId();
        if (!java.util.Objects.equals(empresaAnteriorId, empresaDestinoId) && hasContractorReferences(proyecto)) {
            throw new SolicitudInvalidaException("No se puede cambiar la empresa de una obra mientras tenga subcontratistas seleccionados o asignados");
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
        if (dto.actualizarPedidosDeObraDias()) proyecto.setPedidosDeObraDias(dto.pedidosDeObraDias());
        if (dto.actualizarSuperficiesGenerales()) {
            proyecto.setSuperficieCubierta(dto.superficieCubierta());
            proyecto.setSuperficieSemicubierta(dto.superficieSemicubierta());
            proyecto.setSuperficieDescubierta(dto.superficieDescubierta());
        }
        proyecto.setEmpresa(dto.empresa());
        if (dto.empresaId() != null) {
            proyecto.setEmpresaRelacionada(empresaRepository.findById(dto.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada")));
        }
        proyecto.setHorasPrevistas(dto.horasPrevistas());
        if (horas == null) throw new IllegalStateException("No está configurado el repositorio de horas");
        Double totalHoras = dto.horasReales();
        if (horas.existsByProyectoId(proyectoId)) {
            Double sumaHoras = horas.sumHorasByProyectoId(proyectoId);
            totalHoras = RegistroHorasRules.roundHours(sumaHoras == null ? 0.0 : sumaHoras);
        }
        proyecto.setHorasReales(totalHoras);
        List<Long> selectedContractorIds = new ArrayList<>(dto.subcontratistaIds() == null
            ? existingContractorIds(proyecto) : dto.subcontratistaIds());
        List<Long> contractorChanges = applyCompanyChanges(empresaDestinoId, dto);
        for (Long contractorId : contractorChanges) {
            if (!selectedContractorIds.contains(contractorId)) selectedContractorIds.add(contractorId);
        }
        proyecto.setSubcontratistas(resolveSelectedSubcontratistas(proyecto, selectedContractorIds));

        // Keep managed collection references to satisfy orphanRemoval semantics.
        if (proyecto.getPlantas() == null) {
            proyecto.setPlantas(new ArrayList<>());
        }
        if (dto.plantas() != null) {
            proyecto.getPlantas().clear();
            proyecto.getPlantas().addAll(mapPlantas(dto.plantas(), proyecto));
        }

        if (proyecto.getRubros() == null) {
            proyecto.setRubros(new ArrayList<>());
        }
        var linkedIds = taskRepository.findByProyectoId(proyectoId).stream().map(t -> t.getRubroId())
            .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (dto.rubros() != null && bloqueosCertificados != null) {
            var avances = taskRepository.findByProyectoId(proyectoId);
            for (RubroObra existing : proyecto.getRubros()) {
                RubroCreateDto requested = existing.getId() == null ? null : dto.rubros().stream()
                    .filter(candidate -> existing.getId().equals(candidate.id())).findFirst().orElse(null);
                bloqueosCertificados.validarCambioActividad(proyectoId, existing, requested, avances);
            }
        }
        if (dto.rubros() != null) {
            for (var requested : dto.rubros()) {
                if (requested.id() == null || !linkedIds.contains(requested.id())) continue;
                var existing = proyecto.getRubros().stream().filter(r -> requested.id().equals(r.getId())).findFirst().orElseThrow();
                if (!java.util.Objects.equals(existing.getUnidad(), requested.unidad())) {
                    throw new SolicitudInvalidaException("No se puede cambiar la unidad de una actividad con tareas vinculadas");
                }
            }
        }
        List<ContratoObra> contratos = dto.contratos() == null ? new ArrayList<>(proyecto.getContratos()) : ContratosObra.aplicar(proyecto, dto.contratos());
        List<RubroObra> updated = dto.rubros() == null ? new ArrayList<>(proyecto.getRubros())
            : mapRubros(dto.rubros(), proyecto, linkedIds, ContratosObra.porId(contratos));
        if (updated.stream().anyMatch(rubro -> rubro.getContrato() != null && !contratos.contains(rubro.getContrato()))) {
            throw new SolicitudInvalidaException("No se puede quitar un contrato asignado a un rubro");
        }
        if (dto.contratos() != null) {
            proyecto.getContratos().removeIf(existing -> !contratos.contains(existing));
            contratos.stream().filter(contrato -> !proyecto.getContratos().contains(contrato)).forEach(proyecto.getContratos()::add);
        }
        var retainedIds = updated.stream().map(RubroObra::getId).filter(java.util.Objects::nonNull).toList();
        if (linkedIds.stream().anyMatch(id -> !retainedIds.contains(id))) {
            throw new SolicitudInvalidaException("No se puede eliminar una actividad con tareas vinculadas");
        }
        for (RubroObra existing : proyecto.getRubros()) {
            if (dto.rubros() == null) continue;
            if (existing.getId() == null || restriccionRepository.countByRubroVinculadoId(existing.getId()) == 0) continue;
            RubroCreateDto requested = dto.rubros() == null ? null : dto.rubros().stream()
                .filter(candidate -> existing.getId().equals(candidate.id())).findFirst().orElse(null);
            if (requested == null) {
                throw new SolicitudInvalidaException("No se puede eliminar una actividad vinculada a restricciones");
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

    private void validateDates(ProyectoUpdateDto dto) {
        if (dto.fechaInicio() != null && dto.fechaFin() != null && dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior al inicio de la obra");
        }
    }

    private List<Long> applyCompanyChanges(Long empresaId, ProyectoUpdateDto dto) {
        if (cambiosEmpresa != null) {
            return cambiosEmpresa.execute(empresaId, dto.costosManoObraModificados(), dto.contratistasObra());
        }
        if ((dto.costosManoObraModificados() != null && !dto.costosManoObraModificados().isEmpty())
                || (dto.contratistasObra() != null && !dto.contratistasObra().isEmpty())) {
            throw new IllegalStateException("No está configurado el guardado empresarial del asistente de obra");
        }
        return List.of();
    }

    private void validateSurfaces(Double... surfaces) {
        for (Double surface : surfaces) {
            if (surface != null && (!Double.isFinite(surface) || surface < 0)) {
                throw new SolicitudInvalidaException("Las superficies generales deben ser números no negativos");
            }
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

    private List<RubroObra> mapRubros(List<RubroCreateDto> rubros, Proyecto proyecto, java.util.Set<Long> linkedIds,
            java.util.Map<Long, ContratoObra> contratos) {
        if (rubros == null) {
            return new ArrayList<>();
        }

        java.util.Set<Long> seen = new java.util.HashSet<>();
        RubroObra[] existentes = new RubroObra[rubros.size()];
        for (int i = 0; i < rubros.size(); i++) {
            RubroCreateDto rubroDto = rubros.get(i);
            if (rubroDto.id() == null) continue;
            if (!seen.add(rubroDto.id())) throw new SolicitudInvalidaException("Actividad duplicada");
            existentes[i] = proyecto.getRubros().stream()
                .filter(r -> rubroDto.id().equals(r.getId())).findFirst()
                .orElseThrow(() -> new SolicitudInvalidaException("La actividad no pertenece a la obra"));
        }
        List<EstructuraRubros.Nodo> nodos = RubroEstructura.resolver(rubros, existentes);
        validateStructureChanges(proyecto, existentes, nodos, seen, linkedIds);

        List<RubroObra> result = new ArrayList<>();
        for (int i = 0; i < rubros.size(); i++) {
            RubroCreateDto rubroDto = rubros.get(i);
            EstructuraRubros.Nodo nodo = nodos.get(i);
            RubroObra rubro = existentes[i] == null ? new RubroObra() : existentes[i];
            rubro.setProyecto(proyecto);
            RubroEstructura.aplicarEstructura(rubro, rubroDto, nodo, contratos);
            if (nodo.hoja()) {
                RubroEstructura.aplicarHoja(rubro, rubroDto, resolveSubcontratista(proyecto, rubroDto.subcontratistaId()));
            } else if (existentes[i] == null || existentes[i].getId() == null || !linkedIds.contains(existentes[i].getId())) {
                // Grouping nodes carry no activity data. A node that already has tasks keeps its data untouched.
                RubroEstructura.limpiarAgrupador(rubro);
            }
            result.add(rubro);
        }
        RubroEstructura.enlazarPadres(result, nodos);
        return RubroEstructura.padresPrimero(result, nodos);
    }

    /** Runs before any row is modified: rows omitted from the list are deleted, so their children cannot stay. */
    private void validateStructureChanges(Proyecto proyecto, RubroObra[] existentes, List<EstructuraRubros.Nodo> nodos,
            java.util.Set<Long> requestedIds, java.util.Set<Long> linkedIds) {
        for (int i = 0; i < existentes.length; i++) {
            RubroObra existente = existentes[i];
            if (existente == null) continue;
            EstructuraRubros.Nodo nodo = nodos.get(i);
            RubroObra previousParent = existente.getParent();
            if (previousParent != null && nodo.parentCodigo() == null && previousParent.getId() != null
                    && !requestedIds.contains(previousParent.getId())) {
                throw new SolicitudInvalidaException("No se puede eliminar un nodo que tiene hijos");
            }
            boolean kindChanged = existente.getTipoNodo() != nodo.tipo();
            if (kindChanged && isInUse(existente, linkedIds)) {
                throw new SolicitudInvalidaException("No se puede cambiar el tipo de una actividad con tareas o restricciones vinculadas");
            }
            boolean variantChanged = nodo.tipo() == TipoNodoRubro.RUBRO && existente.getTipoNodo() == TipoNodoRubro.RUBRO
                && existente.getVarianteEstructura() != nodo.variante();
            if (!variantChanged) continue;
            for (RubroObra other : proyecto.getRubros()) {
                RubroObra root = other == existente ? null : EstructuraRubros.raiz(other, proyecto.getRubros());
                boolean belongs = root != null && (root == existente
                    || (existente.getId() != null && existente.getId().equals(root.getId())));
                if (belongs && isInUse(other, linkedIds)) {
                    throw new SolicitudInvalidaException("No se puede cambiar la variante de un rubro con actividades con tareas o restricciones vinculadas");
                }
            }
        }
    }

    private boolean isInUse(RubroObra rubro, java.util.Set<Long> linkedIds) {
        return rubro.getId() != null
            && (linkedIds.contains(rubro.getId()) || restriccionRepository.countByRubroVinculadoId(rubro.getId()) > 0);
    }

    @SuppressWarnings("null")
    private CatalogoEmpresa resolveSubcontratista(Proyecto proyecto, Long subcontratistaId) {
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
            if (!selected) throw new SolicitudInvalidaException("No se puede quitar un subcontratista que sigue asignado a un subrubro");
        }
    }

    private java.util.Set<CatalogoEmpresa> resolveSelectedSubcontratistas(Proyecto proyecto, List<Long> ids) {
        java.util.Set<Long> unique = new java.util.HashSet<>(ids);
        if (unique.size() != ids.size()) throw new SolicitudInvalidaException("Hay subcontratistas duplicados");
        Long companyId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        return unique.stream().map(id -> catalogoRepository.findById(id)
            .filter(item -> companyId != null && companyId.equals(item.getEmpresa().getId())
                && "SUBCONTRATISTA".equalsIgnoreCase(item.getTipo()))
            .orElseThrow(() -> new SolicitudInvalidaException("El subcontratista no pertenece a la empresa de la obra")))
            .collect(java.util.stream.Collectors.toSet());
    }

    @SuppressWarnings("null")
    private boolean hasContractorReferences(Proyecto proyecto) {
        return (proyecto.getSubcontratistas() != null && !proyecto.getSubcontratistas().isEmpty())
            || (proyecto.getRubros() != null && proyecto.getRubros().stream()
                .anyMatch(rubro -> rubro.getSubcontratista() != null));
    }
}