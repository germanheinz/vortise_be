package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRestriccionUseCase {
    private final RestriccionObraRepository restriccionRepository;

    public UpdateRestriccionUseCase(RestriccionObraRepository restriccionRepository) {
        this.restriccionRepository = restriccionRepository;
    }

    @Transactional
    public RestriccionDto execute(Long restriccionId, RestriccionUpdateDto dto) {
        RestriccionObra restriccion = restriccionRepository.findById(restriccionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Restriccion no encontrada"));

        boolean datesChanged = dto.actualizarFechas() || dto.fechaSolicitud() != null
            || dto.fechaMaximaEntrega() != null || dto.fechaRealEntrega() != null;
        var solicitud = dto.actualizarFechas() || dto.fechaSolicitud() != null ? dto.fechaSolicitud() : restriccion.getFechaSolicitud();
        var maxima = dto.actualizarFechas() || dto.fechaMaximaEntrega() != null ? dto.fechaMaximaEntrega() : restriccion.getFechaMaximaEntrega();
        var real = dto.actualizarFechas() || dto.fechaRealEntrega() != null ? dto.fechaRealEntrega() : restriccion.getFechaRealEntrega();
        if (datesChanged) RestriccionPlanning.validateDates(solicitud, maxima, real);
        String area = dto.area() == null ? restriccion.getArea() : RestriccionPlanning.area(dto.area());

        restriccion.setArea(area);
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
        }
        if (dto.orden() != null) {
            restriccion.setOrden(dto.orden());
        }

        return RestriccionMapper.toDto(restriccionRepository.save(restriccion));
    }
}