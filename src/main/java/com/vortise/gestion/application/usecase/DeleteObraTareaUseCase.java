package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeleteObraTareaUseCase {
    private final ObraTareaRepository obraTareaRepository;
    private final CertificadoBloqueos bloqueos;

    @Autowired
    public DeleteObraTareaUseCase(ObraTareaRepository obraTareaRepository, CertificadoBloqueos bloqueos) {
        this.obraTareaRepository = obraTareaRepository;
        this.bloqueos = bloqueos;
    }

    public DeleteObraTareaUseCase(ObraTareaRepository obraTareaRepository) {
        this(obraTareaRepository, null);
    }

    public void execute(Long proyectoId, Long tareaId) {
        ObraTarea tarea = obraTareaRepository.findById(tareaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));

        if (tarea.getProyecto() == null || !proyectoId.equals(tarea.getProyecto().getId())) {
            throw new SolicitudInvalidaException("La tarea no pertenece a la obra indicada");
        }
        if (bloqueos != null) bloqueos.validarAvance(proyectoId, tarea.getActividad(), tarea.getFecha());

        obraTareaRepository.deleteById(tareaId);
    }
}