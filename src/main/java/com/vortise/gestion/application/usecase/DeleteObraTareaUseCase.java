package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteObraTareaUseCase {
    private final ObraTareaRepository obraTareaRepository;

    public DeleteObraTareaUseCase(ObraTareaRepository obraTareaRepository) {
        this.obraTareaRepository = obraTareaRepository;
    }

    public void execute(Long proyectoId, Long tareaId) {
        ObraTarea tarea = obraTareaRepository.findById(tareaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));

        if (tarea.getProyecto() == null || !proyectoId.equals(tarea.getProyecto().getId())) {
            throw new SolicitudInvalidaException("La tarea no pertenece a la obra indicada");
        }

        obraTareaRepository.deleteById(tareaId);
    }
}