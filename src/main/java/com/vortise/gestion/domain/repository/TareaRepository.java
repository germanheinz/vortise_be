/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Tarea;
import java.util.List;
import java.util.Optional;

public interface TareaRepository {
    public List<Tarea> findByEntregaId(Long var1);

    public List<Tarea> findByEntregaProyectoId(Long var1);

    public Optional<Tarea> findById(Long var1);

    public Tarea save(Tarea var1);

    public void deleteById(Long var1);
}
