package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetObraTareasUseCase {
    private final ObraTareaRepository obraTareaRepository;

    public GetObraTareasUseCase(ObraTareaRepository obraTareaRepository) {
        this.obraTareaRepository = obraTareaRepository;
    }

    public List<ObraTareaDto> execute(Long proyectoId) {
        return obraTareaRepository.findByProyectoId(proyectoId).stream().map(CreateObraTareaUseCase::toDto).toList();
    }
}