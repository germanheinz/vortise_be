package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRestriccionUseCase {
    private final RestriccionObraRepository restriccionRepository;
    private final RubroObraRepository rubroRepository;

    public UpdateRestriccionUseCase(RestriccionObraRepository restriccionRepository, RubroObraRepository rubroRepository) {
        this.restriccionRepository = restriccionRepository;
        this.rubroRepository = rubroRepository;
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
        RubroObra rubro = dto.rubroId() == null ? null : rubroRepository.findById(dto.rubroId())
            .filter(candidate -> candidate.getProyecto().getId().equals(restriccion.getProyecto().getId()))
            .filter(candidate -> candidate.getSubRubro() != null && !candidate.getSubRubro().isBlank())
            .orElseThrow(() -> new SolicitudInvalidaException("La actividad debe ser un subrubro de la misma obra"));

        restriccion.setArea(area);
        if (rubro != null) {
            restriccion.setRubroVinculado(rubro);
            restriccion.setNivel(rubro.getNivel());
            restriccion.setUnidad(rubro.getUnidad());
            restriccion.setRubro(java.util.stream.Stream.of(rubro.getRubro(), rubro.getSubRubro())
                .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining(" · ")));
        }
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