/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Proyecto;
import java.util.List;
import java.util.Optional;

public interface ProyectoRepository {
    public List<Proyecto> findAll();

    public Optional<Proyecto> findById(Long var1);

    public Proyecto save(Proyecto var1);

    public void deleteById(Long var1);

    public long count();
}
