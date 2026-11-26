package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.exception.ReglaDeEstadoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestriccionService {
    private final RestriccionObraRepository restriccionRepository;
    private final ProyectoRepository proyectoRepository;
    private final RubroObraRepository rubroRepository;

    private final CurrentIdentity identity;

    public RestriccionService(RestriccionObraRepository restriccionRepository, ProyectoRepository proyectoRepository, RubroObraRepository rubroRepository, CurrentIdentity identity) {
        this.restriccionRepository = restriccionRepository;
        this.proyectoRepository = proyectoRepository;
        this.rubroRepository = rubroRepository;
        this.identity = identity;
    }

    public RestriccionDto crear(Long proyectoId, RestriccionCreateDto dto) {
        RestriccionPlanning.validateDates(dto.fechaSolicitud(), dto.fechaMaximaEntrega(), dto.fechaRealEntrega());
        String area = RestriccionPlanning.area(dto.area());
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        requireWithinUploadDeadline(proyecto);
        RubroObra rubro = RestriccionVinculoEstructura.resolver(dto.rubroId(), proyectoId, rubroRepository);

        RestriccionObra restriccion = new RestriccionObra();
        restriccion.setProyecto(proyecto);
        restriccion.setRestriccion(dto.restriccion());
        restriccion.setTipo(dto.tipo());
        restriccion.setSector(dto.sector());
        if (rubro != null) RestriccionVinculoEstructura.aplicar(restriccion, rubro);
        restriccion.setResponsable(dto.responsable());
        restriccion.setArea(area);
        restriccion.setFechaSolicitud(dto.fechaSolicitud());
        restriccion.setFechaMaximaEntrega(dto.fechaMaximaEntrega());
        restriccion.setFechaRealEntrega(dto.fechaRealEntrega());
        restriccion.setCarga(dto.carga());
        restriccion.setPrioridad(dto.prioridad());
        restriccion.setEstado(dto.estado());
        restriccion.setOrden(0);

        return RestriccionMapper.toDto(restriccionRepository.save(restriccion));
    }

    // Same rule as the frontend: new restrictions are accepted until start date + pedidosDeObraDias (Buenos Aires time).
    private static void requireWithinUploadDeadline(Proyecto proyecto) {
        if (proyecto.getFechaInicio() == null || proyecto.getPedidosDeObraDias() == null) return;
        var deadline = proyecto.getFechaInicio().plusDays(proyecto.getPedidosDeObraDias());
        if (java.time.LocalDate.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires")).isAfter(deadline)) {
            throw new SolicitudInvalidaException("Venció el plazo para cargar restricciones de esta obra (" + deadline + ").");
        }
    }

    @Transactional
    public RestriccionDto actualizar(Long restriccionId, RestriccionUpdateDto dto) {
        RestriccionObra restriccion = restriccionRepository.findById(restriccionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Restriccion no encontrada"));
        if (identity != null && identity.isJefeDeObra()) requireReleaseOnly(restriccion, dto);

        boolean datesChanged = dto.actualizarFechas() || dto.fechaSolicitud() != null
            || dto.fechaMaximaEntrega() != null || dto.fechaRealEntrega() != null;
        var solicitud = dto.actualizarFechas() || dto.fechaSolicitud() != null ? dto.fechaSolicitud() : restriccion.getFechaSolicitud();
        var maxima = dto.actualizarFechas() || dto.fechaMaximaEntrega() != null ? dto.fechaMaximaEntrega() : restriccion.getFechaMaximaEntrega();
        var real = dto.actualizarFechas() || dto.fechaRealEntrega() != null ? dto.fechaRealEntrega() : restriccion.getFechaRealEntrega();
        if (datesChanged) RestriccionPlanning.validateDates(solicitud, maxima, real);
        String area = dto.area() == null ? restriccion.getArea() : RestriccionPlanning.area(dto.area());
        RubroObra rubro = RestriccionVinculoEstructura.resolver(dto.rubroId(), restriccion.getProyecto().getId(), rubroRepository);

        restriccion.setArea(area);
        if (rubro != null) RestriccionVinculoEstructura.aplicar(restriccion, rubro);
        if (dto.responsable() != null) {
            restriccion.setResponsable(dto.responsable().isBlank() ? null : dto.responsable().trim());
        }
        if (datesChanged) {
            restriccion.setFechaSolicitud(solicitud);
            restriccion.setFechaMaximaEntrega(maxima);
            restriccion.setFechaRealEntrega(real);
        }
        if (dto.prioridad() != null) {
            restriccion.setPrioridad(dto.prioridad());
        }
        if (dto.estado() != null) {
            restriccion.setEstado(dto.estado());
            if (!datesChanged) syncResolutionDate(restriccion);
        }
        if (dto.orden() != null) {
            restriccion.setOrden(dto.orden());
        }

        return RestriccionMapper.toDto(restriccionRepository.save(restriccion));
    }

    // A JEFE_DE_OBRA may only change the state (the board also sends the card position), and only once:
    // a resolved restriction cannot be changed again.
    private static void requireReleaseOnly(RestriccionObra restriccion, RestriccionUpdateDto dto) {
        boolean onlyState = dto.estado() != null && dto.prioridad() == null && dto.area() == null
            && dto.responsable() == null && dto.rubroId() == null && !dto.actualizarFechas()
            && dto.fechaSolicitud() == null && dto.fechaMaximaEntrega() == null && dto.fechaRealEntrega() == null;
        if (!onlyState) {
            throw new AccessDeniedException("Solo Gerencia o Coordinación de obras puede editar los datos de la restricción.");
        }
        if ("RESUELTO".equalsIgnoreCase(restriccion.getEstado())) {
            throw new ReglaDeEstadoException("Una restricción resuelta solo puede modificarla Gerencia o Coordinación de obras.");
        }
    }

    // The real date is the resolution date: set it on resolve, clear it on reopen.
    private static void syncResolutionDate(RestriccionObra restriccion) {
        if (!"RESUELTO".equalsIgnoreCase(restriccion.getEstado())) {
            restriccion.setFechaRealEntrega(null);
            return;
        }
        if (restriccion.getFechaRealEntrega() != null) return;
        LocalDate today = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        LocalDate requested = restriccion.getFechaSolicitud();
        restriccion.setFechaRealEntrega(requested != null && requested.isAfter(today) ? requested : today);
    }

    public List<RestriccionDto> listarPorProyecto(Long proyectoId) {
        return restriccionRepository.findByProyectoId(proyectoId).stream().map(RestriccionMapper::toDto).toList();
    }

    public List<RestriccionDto> listarPorEmpresaActual() {
        Long empresaId = identity.empresaId();
        return restriccionRepository.findAll().stream()
            .filter(item -> item.getProyecto() != null && item.getProyecto().getEmpresaRelacionada() != null)
            .filter(item -> empresaId.equals(item.getProyecto().getEmpresaRelacionada().getId()))
            .map(RestriccionMapper::toDto)
            .toList();
    }

    public RestriccionDto obtener(Long restriccionId) {
        RestriccionObra restriccion = restriccionRepository.findById(restriccionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Restriccion no encontrada"));
        return RestriccionMapper.toDto(restriccion);
    }

    public void eliminar(Long restriccionId) {
        if (restriccionRepository.findById(restriccionId).isEmpty()) {
            throw new RecursoNoEncontradoException("Restriccion no encontrada");
        }

        restriccionRepository.deleteById(restriccionId);
    }
}
