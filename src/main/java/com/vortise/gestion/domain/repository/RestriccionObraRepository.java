package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RestriccionObra;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RestriccionObraRepository {
    @Query("select r.proyecto.empresaRelacionada.id as empresaId, count(r) as cantidad from RestriccionObra r where r.proyecto.empresaRelacionada.id in :empresaIds group by r.proyecto.empresaRelacionada.id")
    List<EmpresaConteoProjection> contarPorEmpresaRelacionada(@Param("empresaIds") Collection<Long> empresaIds);

    List<RestriccionObra> findAll();

    List<RestriccionObra> findByProyectoId(Long proyectoId);

    Optional<RestriccionObra> findById(Long restriccionId);

    RestriccionObra save(RestriccionObra restriccion);

    void deleteById(Long restriccionId);
}