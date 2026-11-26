package com.vortise.gestion.service.proyecto;

import com.vortise.gestion.service.empresa.ConfiguracionEmpresaObraService;
import com.vortise.gestion.application.dto.CalendarioObraDto;
import com.vortise.gestion.application.dto.PaginaDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.helper.ContratosObra;
import com.vortise.gestion.application.helper.DatosProyecto;
import com.vortise.gestion.application.helper.RubroEstructura;
import com.vortise.gestion.application.helper.SubcontratistasProyecto;
import com.vortise.gestion.application.mapper.ProyectoMapper;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.ContratoObra;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RubroObra;
import com.vortise.gestion.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import com.vortise.gestion.domain.rule.EstructuraRubros;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyectoService {
    private final ProyectoRepository projects;
    private final EmpresaRepository companies;
    private final CurrentIdentity identity;
    private final SubcontratistasProyecto subcontratistas;
    private final ConfiguracionEmpresaObraService cambiosEmpresa;

    public ProyectoService(ProyectoRepository projects, EmpresaRepository companies,
            CatalogoEmpresaRepository catalogoRepository, CurrentIdentity identity, ConfiguracionEmpresaObraService cambiosEmpresa) {
        this.projects = projects;
        this.companies = companies;
        this.identity = identity;
        this.subcontratistas = new SubcontratistasProyecto(catalogoRepository);
        this.cambiosEmpresa = cambiosEmpresa;
    }

    @Transactional(readOnly = true)
    public PaginaDto<ProyectoDto> paginar(Long empresaId, String q, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || q == null || q.length() > 160) {
            throw new SolicitudInvalidaException("Parámetros de paginación inválidos");
        }
        Long companyId = empresaId == null ? identity.empresaId() : empresaId;
        identity.requireEmpresa(companyId);
        if (companies.findById(companyId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
        var result = projects.buscarPagina(companyId, q.trim(), page, size);
        return new PaginaDto<>(result.content().stream().map(ProyectoMapper::toDetailDto).toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public List<ProyectoDto> listarDeEmpresaActual() {
        return projects.findByEmpresaRelacionadaId(identity.empresaId()).stream()
            .map(ProyectoMapper::toDetailDto).toList();
    }

    public ProyectoDto obtener(Long proyectoId) {
        var proyecto = projects.findById(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        return ProyectoMapper.toDetailDto(proyecto);
    }

    public List<ProyectoDto> listarResumenesPorEmpresa(Long empresaId) {
        if (companies.findById(empresaId).isEmpty()) {
            throw new RecursoNoEncontradoException("Empresa no encontrada");
        }
        return projects.findByEmpresaRelacionadaId(empresaId).stream()
            .map(ProyectoMapper::toSummaryDto)
            .toList();
    }

    public void eliminar(Long proyectoId) {
        if (projects.findById(proyectoId).isEmpty()) {
            throw new RecursoNoEncontradoException("Proyecto no encontrado");
        }

        projects.deleteById(proyectoId);
    }

    @SuppressWarnings("null")
    @Transactional
    public ProyectoDto crear(ProyectoCreateDto dto) {
        DatosProyecto.validarFechas(dto.fechaInicio(), dto.fechaFin());
        DatosProyecto.validarSuperficies(dto.superficieCubierta(), dto.superficieSemicubierta(), dto.superficieDescubierta());
        validatePedidosDeObraDias(dto.pedidosDeObraDias());
        identity.requireEmpresa(dto.empresaId());
        Proyecto proyecto = Proyecto.builder().nombre(dto.nombre()).descripcion(dto.descripcion()).presupuestoUsd(dto.presupuestoUsd()).build();
        proyecto.setDireccion(dto.direccion());
        proyecto.setNumeroProyecto(dto.numeroProyecto());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setFechaFin(dto.fechaFin());
        proyecto.setResponsablesObra(dto.responsablesObra());
        proyecto.setCategoriasManoObra(dto.categoriasManoObra());
        proyecto.setCantidadSectores(dto.cantidadSectores());
        proyecto.setPedidosDeObraDias(dto.pedidosDeObraDias());
        DatosProyecto.aplicarSuperficies(proyecto, dto.superficieCubierta(), dto.superficieSemicubierta(), dto.superficieDescubierta());
        proyecto.setEmpresa(dto.empresa());
        if (dto.empresaId() != null) {
            proyecto.setEmpresaRelacionada(companies.bloquearPorId(dto.empresaId())
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
        Proyecto saved = projects.save(proyecto);
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

    /** Sin calendario en la solicitud, la obra parte del calendario de su empresa (o de los valores por defecto de la empresa). */
    private void aplicarCalendario(Proyecto proyecto, CalendarioObraDto dto) {
        if (dto != null) {
            DatosProyecto.aplicarCalendario(proyecto, dto);
            return;
        }
        var empresa = proyecto.getEmpresaRelacionada();
        var item = empresa == null ? null : empresa.getCalendarioLaboral();
        if (item != null) {
            proyecto.setCalendarioLaboral(item.getHorasSabado() != null && item.getHorasSabado() > 0,
            item.isTrabajaDomingo(), item.isPagaDobleFeriado(), item.isPagaDobleNoLaborable());
        }
    }

    private void addDistinct(List<Long> selectedIds, List<Long> additionalIds) {
        additionalIds.stream().filter(id -> !selectedIds.contains(id)).forEach(selectedIds::add);
    }
}
