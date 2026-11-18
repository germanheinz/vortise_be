package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.exception.ReglaDeEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class UpdateRestriccionUseCase {
    private final RestriccionObraRepository restriccionRepository;
    private final RubroObraRepository rubroRepository;
    private final CurrentIdentity identity;

    @Autowired
    public UpdateRestriccionUseCase(RestriccionObraRepository restriccionRepository, RubroObraRepository rubroRepository, CurrentIdentity identity) {
        this.restriccionRepository = restriccionRepository;
        this.rubroRepository = rubroRepository;
        this.identity = identity;
    }

    public UpdateRestriccionUseCase(RestriccionObraRepository restriccionRepository, RubroObraRepository rubroRepository) {
        this(restriccionRepository, rubroRepository, null);
    }

    @Transactional
    public RestriccionDto execute(Long restriccionId, RestriccionUpdateDto dto) {
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
}