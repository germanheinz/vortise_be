package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateRestriccionUseCase {
    private final RestriccionObraRepository restriccionRepository;
    private final ProyectoRepository proyectoRepository;
    private final RubroObraRepository rubroRepository;

    public CreateRestriccionUseCase(RestriccionObraRepository restriccionRepository, ProyectoRepository proyectoRepository, RubroObraRepository rubroRepository) {
        this.restriccionRepository = restriccionRepository;
        this.proyectoRepository = proyectoRepository;
        this.rubroRepository = rubroRepository;
    }

    public RestriccionDto execute(Long proyectoId, RestriccionCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        RubroObra rubro = rubroRepository.findById(dto.rubroId())
            .filter(candidate -> candidate.getProyecto().getId().equals(proyectoId))
            .filter(candidate -> candidate.getSubRubro() != null && !candidate.getSubRubro().isBlank())
            .orElseThrow(() -> new IllegalArgumentException("La actividad debe ser un subrubro de la misma obra"));

        RestriccionObra restriccion = new RestriccionObra();
        restriccion.setProyecto(proyecto);
        restriccion.setRestriccion(dto.restriccion());
        restriccion.setTipo(dto.tipo());
        restriccion.setSector(dto.sector());
        restriccion.setRubroVinculado(rubro);
        restriccion.setNivel(rubro.getNivel());
        restriccion.setUnidad(rubro.getUnidad());
        restriccion.setRubro(java.util.stream.Stream.of(rubro.getRubro(), rubro.getSubRubro())
            .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining(" · ")));
        restriccion.setResponsable(dto.responsable());
        restriccion.setFechaSolicitud(dto.fechaSolicitud());
        restriccion.setFechaMaximaEntrega(dto.fechaMaximaEntrega());
        restriccion.setFechaRealEntrega(dto.fechaRealEntrega());
        restriccion.setCarga(dto.carga());
        restriccion.setPrioridad(dto.prioridad());
        restriccion.setEstado(dto.estado());
        restriccion.setOrden(0);

        return RestriccionMapper.toDto(restriccionRepository.save(restriccion));
    }
}