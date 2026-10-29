/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Proyecto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository {
    Optional<Proyecto> bloquearPorId(Long id);
    PageSlice<Proyecto> buscarPagina(Long empresaId, String q, int page, int size);
    public List<Proyecto> findAll();

    public Optional<Proyecto> findById(Long var1);

    public Proyecto save(Proyecto var1);

    public void deleteById(Long var1);

    public long count();

    public long countByEmpresaRelacionadaId(Long empresaId);

    @Query("select p.empresaRelacionada.id as empresaId, count(p) as cantidad from Proyecto p where p.empresaRelacionada.id in :empresaIds group by p.empresaRelacionada.id")
    List<EmpresaConteoProjection> contarPorEmpresaRelacionada(@Param("empresaIds") Collection<Long> empresaIds);

    public List<Proyecto> findByEmpresaRelacionadaId(Long empresaId);
}
