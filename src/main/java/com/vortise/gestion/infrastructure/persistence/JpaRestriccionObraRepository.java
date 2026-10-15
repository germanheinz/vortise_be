package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRestriccionObraRepository extends JpaRepository<RestriccionObra, Long>, RestriccionObraRepository {
    // Access checks and DTO mapping run after the repository session has closed.
    @Override
    @EntityGraph(attributePaths = {"proyecto", "proyecto.empresaRelacionada"})
    Optional<RestriccionObra> findById(Long id);

    @Override
    List<RestriccionObra> findAll();

    @Override
    List<RestriccionObra> findByProyectoId(Long proyectoId);
}
