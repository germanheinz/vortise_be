package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteProyectoUseCase {
    private final ProyectoRepository proyectoRepository;

    public DeleteProyectoUseCase(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    public void execute(Long proyectoId) {
        if (proyectoRepository.findById(proyectoId).isEmpty()) {
            throw new RecursoNoEncontradoException("Proyecto no encontrado");
        }

        proyectoRepository.deleteById(proyectoId);
    }
}