package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import org.springframework.stereotype.Service;

@Service
public class GetRegistroHorasByIdUseCase {
    private final RegistroHorasRepository registroHorasRepository;

    public GetRegistroHorasByIdUseCase(RegistroHorasRepository registroHorasRepository) {
        this.registroHorasRepository = registroHorasRepository;
    }

    public RegistroHorasDto execute(Long id) {
        return registroHorasRepository.findById(id).map(RegistroHorasMapper::toDto)
            .orElseThrow(() -> new IllegalArgumentException("Registro de horas no encontrado"));
    }
}