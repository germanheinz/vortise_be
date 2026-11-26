/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CalendarioObraDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.domain.model.ContratoObra;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.service.EstructuraRubros;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProyectoUseCase {
    private final ProyectoRepository proyectoRepository;
    private final EmpresaRepository empresaRepository;
    private final SubcontratistasProyecto subcontratistas;
    private final CurrentIdentity currentTenant;
    private final AplicarCambiosEmpresaObraUseCase cambiosEmpresa;
    private final CalendarioLaboralEmpresaRepository calendarios;

    @SuppressWarnings("null")
    @Transactional
    public ProyectoDto execute(ProyectoCreateDto dto) {
        DatosProyecto.validarFechas(dto.fechaInicio(), dto.fechaFin());
        DatosProyecto.validarSuperficies(dto.superficieCubierta(), dto.superficieSemicubierta(), dto.superficieDescubierta());
        validatePedidosDeObraDias(dto.pedidosDeObraDias());
        currentTenant.requireEmpresa(dto.empresaId());
        Proyecto proyecto = Proyecto.builder().nombre(dto.nombre()).descripcion(dto.descripcion()).presupuestoUsd(dto.presupuestoUsd()).build();
        proyecto.setDireccion(dto.direccion());
        proyecto.setNumeroProyecto(dto.numeroProyecto());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setFechaFin(dto.fechaFin());
        proyecto.setResponsablesObra(dto.responsablesObra());
        proyecto.setCategoriasManoObra(dto.categoriasManoObra());
        proyecto.setCantidadSectores(dto.cantidadSectores());
        proyecto.setPedidosDeObraDias(dto.pedidosDeObraDias());
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
        aplicarCalendario(proyecto, dto.calendarioLaboral());
        proyecto.setPlantas(DatosProyecto.mapearPlantas(dto.plantas(), proyecto));
        List<Long> selectedIds = new ArrayList<>(dto.subcontratistaIds() == null ? selectedContractorIds(dto) : dto.subcontratistaIds());
        addDistinct(selectedIds, cambiosEmpresa.execute(dto.empresaId(), dto.costosManoObraModificados(), dto.contratistasObra()));
        proyecto.setSubcontratistas(subcontratistas.resolverSeleccion(proyecto, selectedIds));
        List<ContratoObra> contratos = dto.contratos() == null ? new ArrayList<>() : ContratosObra.aplicar(proyecto, dto.contratos());
        proyecto.setContratos(contratos);
        proyecto.setRubros(mapRubros(dto.rubros(), proyecto));
        Proyecto saved = this.proyectoRepository.save(proyecto);
        return ProyectoMapper.toDetailDto(saved);
    }

    private void validatePedidosDeObraDias(Integer dias) {
        if (dias != null && dias < 0) throw new SolicitudInvalidaException("Pedidos de obra debe ser un entero no negativo");
    }

    private List<RubroObra> mapRubros(List<RubroCreateDto> rubros, Proyecto proyecto) {
        if (rubros == null) {
            return new ArrayList<>();
        }
        List<EstructuraRubros.Nodo> nodos = RubroEstructura.resolver(rubros, null);
        List<RubroObra> result = new ArrayList<>();
        for (int i = 0; i < rubros.size(); i++) {
            RubroCreateDto rubroDto = rubros.get(i);
            EstructuraRubros.Nodo nodo = nodos.get(i);
            RubroObra rubro = new RubroObra();
            rubro.setProyecto(proyecto);
            RubroEstructura.aplicarEstructura(rubro, rubroDto, nodo, java.util.Map.of());
            if (nodo.hoja()) {
                RubroEstructura.aplicarHoja(rubro, rubroDto, subcontratistas.resolverAsignado(proyecto, rubroDto.subcontratistaId()));
            } else {
                RubroEstructura.limpiarAgrupador(rubro);
                RubroEstructura.aplicarPlanificacionAgrupador(rubro, rubroDto);
            }
            result.add(rubro);
        }
        RubroEstructura.enlazarPadres(result, nodos);
        return RubroEstructura.padresPrimero(result, nodos);
    }

    @SuppressWarnings("null")
    private List<Long> selectedContractorIds(ProyectoCreateDto dto) {
        if (dto.subcontratistaIds() != null) return dto.subcontratistaIds();
        if (dto.rubros() == null) return List.of();
        return dto.rubros().stream().map(RubroCreateDto::subcontratistaId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    public CreateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository,
            CurrentIdentity currentTenant, AplicarCambiosEmpresaObraUseCase cambiosEmpresa) {
        this(proyectoRepository, empresaRepository, catalogoRepository, currentTenant, cambiosEmpresa, null);
    }

    @Autowired
    public CreateProyectoUseCase(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository, CatalogoEmpresaRepository catalogoRepository,
            CurrentIdentity currentTenant, AplicarCambiosEmpresaObraUseCase cambiosEmpresa, CalendarioLaboralEmpresaRepository calendarios) {
        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
        this.subcontratistas = new SubcontratistasProyecto(catalogoRepository);
        this.currentTenant = currentTenant;
        this.cambiosEmpresa = cambiosEmpresa;
        this.calendarios = calendarios;
    }

    /** Sin calendario en la solicitud, la obra parte del calendario de su empresa (o de los valores por defecto de la empresa). */
    private void aplicarCalendario(Proyecto proyecto, CalendarioObraDto dto) {
        if (dto != null) {
            proyecto.setCalendarioLaboral(dto.trabajaSabado(), dto.trabajaDomingo(), dto.pagaDobleFeriado(), dto.pagaDobleNoLaborable());
            return;
        }
        Long empresaId = proyecto.getEmpresaRelacionada() == null ? null : proyecto.getEmpresaRelacionada().getId();
        var calendario = empresaId == null || calendarios == null ? java.util.Optional.<CalendarioLaboralEmpresa>empty()
            : calendarios.findByEmpresaId(empresaId);
        calendario.ifPresent(item -> proyecto.setCalendarioLaboral(item.getHorasSabado() != null && item.getHorasSabado() > 0,
            item.isTrabajaDomingo(), item.isPagaDobleFeriado(), item.isPagaDobleNoLaborable()));
    }

    private void addDistinct(List<Long> selectedIds, List<Long> additionalIds) {
        additionalIds.stream().filter(id -> !selectedIds.contains(id)).forEach(selectedIds::add);
    }
}
