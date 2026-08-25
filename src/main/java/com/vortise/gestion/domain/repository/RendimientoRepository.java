/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Rendimiento;
import java.util.List;
import java.util.Optional;

public interface RendimientoRepository {
    public List<Rendimiento> findAll();

    public Optional<Rendimiento> findById(Long var1);

    public Rendimiento save(Rendimiento var1);

    public void deleteById(Long var1);
}
