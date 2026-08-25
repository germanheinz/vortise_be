package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRestriccionObraRepository extends JpaRepository<RestriccionObra, Long>, RestriccionObraRepository {
    @Override
    List<RestriccionObra> findAll();

    @Override
    List<RestriccionObra> findByProyectoId(Long proyectoId);
}