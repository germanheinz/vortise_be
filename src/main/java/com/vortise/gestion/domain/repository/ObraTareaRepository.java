package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.ObraTarea;
import java.util.List;
import java.util.Optional;

public interface ObraTareaRepository {
    List<ObraTarea> findByProyectoId(Long proyectoId);

    Optional<ObraTarea> findById(Long tareaId);

    ObraTarea save(ObraTarea tarea);

    void deleteById(Long tareaId);
}