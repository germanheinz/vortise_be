/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Planta;
import java.util.List;
import java.util.Optional;

public interface PlantaRepository {
    public List<Planta> findByProyectoId(Long var1);

    public Optional<Planta> findById(Long var1);

    public Planta save(Planta var1);

    public void deleteById(Long var1);
}
