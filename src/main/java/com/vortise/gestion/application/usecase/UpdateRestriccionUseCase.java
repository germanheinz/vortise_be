package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateRestriccionUseCase {
    private final RestriccionObraRepository restriccionRepository;

    public UpdateRestriccionUseCase(RestriccionObraRepository restriccionRepository) {
        this.restriccionRepository = restriccionRepository;
    }

    public RestriccionDto execute(Long restriccionId, RestriccionUpdateDto dto) {
        RestriccionObra restriccion = restriccionRepository.findById(restriccionId)
            .orElseThrow(() -> new IllegalArgumentException("Restriccion no encontrada"));

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