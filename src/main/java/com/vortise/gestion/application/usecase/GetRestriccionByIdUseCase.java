package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import org.springframework.stereotype.Service;

@Service
public class GetRestriccionByIdUseCase {
    private final RestriccionObraRepository restriccionRepository;

    public GetRestriccionByIdUseCase(RestriccionObraRepository restriccionRepository) {
        this.restriccionRepository = restriccionRepository;
    }

    public RestriccionDto execute(Long restriccionId) {
        RestriccionObra restriccion = restriccionRepository.findById(restriccionId)
            .orElseThrow(() -> new IllegalArgumentException("Restriccion no encontrada"));
        return RestriccionMapper.toDto(restriccion);
    }
}