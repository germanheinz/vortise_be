/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.PresupuestoLinea;
import java.util.List;
import java.util.Optional;

public interface PresupuestoLineaRepository {
    public List<PresupuestoLinea> findByProyectoId(Long var1);

    public Optional<PresupuestoLinea> findById(Long var1);

    public PresupuestoLinea save(PresupuestoLinea var1);

    public void deleteById(Long var1);
}
