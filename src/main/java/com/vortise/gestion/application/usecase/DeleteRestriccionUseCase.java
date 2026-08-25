package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteRestriccionUseCase {
    private final RestriccionObraRepository restriccionObraRepository;

    public DeleteRestriccionUseCase(RestriccionObraRepository restriccionObraRepository) {
        this.restriccionObraRepository = restriccionObraRepository;
    }

    public void execute(Long restriccionId) {
        if (restriccionObraRepository.findById(restriccionId).isEmpty()) {
            throw new IllegalArgumentException("Restriccion no encontrada");
        }

        restriccionObraRepository.deleteById(restriccionId);
    }
}