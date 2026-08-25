package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RestriccionObra;
import java.util.List;
import java.util.Optional;

public interface RestriccionObraRepository {
    List<RestriccionObra> findAll();

    List<RestriccionObra> findByProyectoId(Long proyectoId);

    Optional<RestriccionObra> findById(Long restriccionId);

    RestriccionObra save(RestriccionObra restriccion);

    void deleteById(Long restriccionId);
}