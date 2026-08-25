package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetRegistrosHorasUseCase {
    private final RegistroHorasRepository registroHorasRepository;

    public GetRegistrosHorasUseCase(RegistroHorasRepository registroHorasRepository) {
        this.registroHorasRepository = registroHorasRepository;
    }

    public List<RegistroHorasDto> execute(Long proyectoId) {
        return registroHorasRepository.findByProyectoId(proyectoId).stream().map(RegistroHorasMapper::toDto).toList();
    }
}