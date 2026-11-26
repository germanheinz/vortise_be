package com.vortise.gestion.repository;

import com.vortise.gestion.model.ObraTarea;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ObraTareaRepository {
    List<ObraTarea> findByProyectoId(Long proyectoId);

    /** Tasks of a company that have an activity assigned, with the data needed to evaluate their compliance. */
    @Query("select t from ObraTarea t join fetch t.proyecto p join fetch t.actividad where p.empresaRelacionada.id = :empresaId")
    List<ObraTarea> findConActividadByEmpresaRelacionadaId(@Param("empresaId") Long empresaId);

    Optional<ObraTarea> findById(Long tareaId);

    ObraTarea save(ObraTarea tarea);

    void deleteById(Long tareaId);
}