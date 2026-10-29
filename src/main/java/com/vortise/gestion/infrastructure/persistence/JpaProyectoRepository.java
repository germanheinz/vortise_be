/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProyectoRepository
extends JpaRepository<Proyecto, Long>,
ProyectoRepository, ProyectoLockRepository {


    @EntityGraph(attributePaths = "empresaRelacionada")
    @Query("select p from Proyecto p where p.empresaRelacionada.id = :empresaId and (:q = '' or lower(p.nombre) like lower(concat('%', :q, '%')) or lower(p.numeroProyecto) like lower(concat('%', :q, '%')) or lower(p.direccion) like lower(concat('%', :q, '%')))")
    org.springframework.data.domain.Page<Proyecto> buscarPaginaJpa(
        @Param("empresaId") Long empresaId,
        @Param("q") String q,
        org.springframework.data.domain.Pageable pageable);

    @Override
    default com.vortise.gestion.domain.repository.PageSlice<Proyecto> buscarPagina(Long empresaId, String q, int page, int size) {
        var result = buscarPaginaJpa(empresaId, q, org.springframework.data.domain.PageRequest.of(page, size,
            org.springframework.data.domain.Sort.by("id")));
        return new com.vortise.gestion.domain.repository.PageSlice<>(result.getContent(), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
