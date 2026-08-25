package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateRestriccionUseCase {
    private final RestriccionObraRepository restriccionRepository;
    private final ProyectoRepository proyectoRepository;

    public CreateRestriccionUseCase(RestriccionObraRepository restriccionRepository, ProyectoRepository proyectoRepository) {
        this.restriccionRepository = restriccionRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public RestriccionDto execute(Long proyectoId, RestriccionCreateDto dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        RestriccionObra restriccion = new RestriccionObra();
        restriccion.setProyecto(proyecto);
        restriccion.setRestriccion(dto.restriccion());
        restriccion.setTipo(dto.tipo());
        restriccion.setSector(dto.sector());
        restriccion.setNivel(dto.nivel());
        restriccion.setUnidad(dto.unidad());
        restriccion.setRubro(dto.rubro());
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