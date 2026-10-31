/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.PlantaCreateDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProyectoUseCase {
    private final ProyectoRepository proyectoRepository;
    private final EmpresaRepository empresaRepository;
    private final CatalogoEmpresaRepository catalogoRepository;
    private final CurrentIdentity currentTenant;
    private final AplicarCambiosEmpresaObraUseCase cambiosEmpresa;

    @SuppressWarnings("null")
    @Transactional
    public ProyectoDto execute(ProyectoCreateDto dto) {
        validateDates(dto);
        validateSurfaces(dto.superficieCubierta(), dto.superficieSemicubierta(), dto.superficieDescubierta());
        currentTenant.requireEmpresa(dto.empresaId());
        Proyecto proyecto = Proyecto.builder().nombre(dto.nombre()).descripcion(dto.descripcion()).presupuestoUsd(dto.presupuestoUsd()).build();
        proyecto.setDireccion(dto.direccion());
        proyecto.setNumeroProyecto(dto.numeroProyecto());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setFechaFin(dto.fechaFin());
        proyecto.setResponsablesObra(dto.responsablesObra());
        proyecto.setCategoriasManoObra(dto.categoriasManoObra());
        proyecto.setCantidadSectores(dto.cantidadSectores());
        proyecto.setSuperficieCubierta(dto.superficieCubierta());
        proyecto.setSuperficieSemicubierta(dto.superficieSemicubierta());
        proyecto.setSuperficieDescubierta(dto.superficieDescubierta());
        proyecto.setEmpresa(dto.empresa());
        if (dto.empresaId() != null) {
            proyecto.setEmpresaRelacionada(empresaRepository.bloquearPorId(dto.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada")));
        }
        proyecto.setHorasPrevistas(dto.horasPrevistas());
        proyecto.setHorasReales(dto.horasReales());
        proyecto.setPlantas(mapPlantas(dto.plantas(), proyecto));
        List<Long> selectedIds = new ArrayList<>(dto.subcontratistaIds() == null ? selectedContractorIds(dto) : dto.subcontratistaIds());
        addDistinct(selectedIds, cambiosEmpresa.execute(dto.empresaId(), dto.costosManoObraModificados(), dto.contratistasObra()));
        proyecto.setSubcontratistas(resolveSelectedSubcontratistas(proyecto, selectedIds));
        proyecto.setRubros(mapRubros(dto.rubros(), proyecto));
        Proyecto saved = this.proyectoRepository.save(proyecto);
        return ProyectoMapper.toDetailDto(saved);
    }

    private void validateDates(ProyectoCreateDto dto) {
        if (dto.fechaInicio() != null && dto.fechaFin() != null && dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior al inicio de la obra");
        }
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

    private List<RubroObra> mapRubros(List<RubroCreateDto> rubros, Proyecto proyecto) {
        if (rubros == null) {
            return new ArrayList<>();
        }
        List<RubroObra> result = new ArrayList<>();
        for (RubroCreateDto rubroDto : rubros) {
            RubroObra rubro = new RubroObra();
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
    private List<Long> selectedContractorIds(ProyectoCreateDto dto) {
        if (dto.subcontratistaIds() != null) return dto.subcontratistaIds();
        if (dto.rubros() == null) return List.of();
        return dto.rubros().stream().map(RubroCreateDto::subcontratistaId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private java.util.Set<CatalogoEmpresa> resolveSelectedSubcontratistas(Proyecto proyecto, List<Long> ids) {
        Long companyId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        java.util.Set<Long> unique = new java.util.HashSet<>(ids);
        if (unique.size() != ids.size()) throw new SolicitudInvalidaException("Hay subcontratistas duplicados");
        return unique.stream().map(id -> catalogoRepository.findById(id)
            .filter(item -> companyId != null && companyId.equals(item.getEmpresa().getId())
                && "SUBCONTRATISTA".equalsIgnoreCase(item.getTipo()))
            .orElseThrow(() -> new SolicitudInvalidaException("El subcontratista no pertenece a la empresa de la obra")))
            .collect(java.util.stream.Collectors.toSet());
    }

    public CreateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository,
            CurrentIdentity currentTenant, AplicarCambiosEmpresaObraUseCase cambiosEmpresa) {
        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
        this.catalogoRepository = catalogoRepository;
        this.currentTenant = currentTenant;
        this.cambiosEmpresa = cambiosEmpresa;
    }

    private void addDistinct(List<Long> selectedIds, List<Long> additionalIds) {
        additionalIds.stream().filter(id -> !selectedIds.contains(id)).forEach(selectedIds::add);
    }
}
