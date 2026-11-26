/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.repository;

import com.vortise.gestion.model.Entrega;
import java.util.List;
import java.util.Optional;

public interface EntregaRepository {
    public List<Entrega> findByProyectoId(Long var1);

    public Optional<Entrega> findById(Long var1);

    public Entrega save(Entrega var1);

    public void deleteById(Long var1);
}
