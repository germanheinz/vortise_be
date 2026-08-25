/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Ingreso;
import java.util.List;
import java.util.Optional;

public interface IngresoRepository {
    public List<Ingreso> findByProyectoId(Long var1);

    public Optional<Ingreso> findById(Long var1);

    public Ingreso save(Ingreso var1);

    public void deleteById(Long var1);
}
