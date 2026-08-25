package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaObraTareaRepository extends JpaRepository<ObraTarea, Long>, ObraTareaRepository {
    @Override
    List<ObraTarea> findByProyectoId(Long proyectoId);
}