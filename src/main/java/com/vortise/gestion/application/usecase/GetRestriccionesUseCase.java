package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetRestriccionesUseCase {
    private final RestriccionObraRepository restriccionRepository;

    public GetRestriccionesUseCase(RestriccionObraRepository restriccionRepository) {
        this.restriccionRepository = restriccionRepository;
    }

    public List<RestriccionDto> execute(Long proyectoId) {
        return restriccionRepository.findByProyectoId(proyectoId).stream().map(RestriccionMapper::toDto).toList();
    }

    public List<RestriccionDto> executeAll() {
        return restriccionRepository.findAll().stream().map(RestriccionMapper::toDto).toList();
    }
}