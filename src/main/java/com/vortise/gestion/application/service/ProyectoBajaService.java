package com.vortise.gestion.application.service;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.stereotype.Service;

@Service
public class ProyectoBajaService {
    private final ProyectoRepository proyectoRepository;

    public ProyectoBajaService(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    public void execute(Long proyectoId) {
        if (proyectoRepository.findById(proyectoId).isEmpty()) {
            throw new RecursoNoEncontradoException("Proyecto no encontrado");
        }

        proyectoRepository.deleteById(proyectoId);
    }
}