package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class GetProyectoByIdUseCase {
    private final ProyectoRepository proyectoRepository;

    public GetProyectoByIdUseCase(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    public ProyectoDto execute(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        return ProyectoMapper.toDetailDto(proyecto);
    }
}