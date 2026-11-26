/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.repository;

import com.vortise.gestion.model.Tarea;
import java.util.Optional;

public interface TareaRepository {
    public Optional<Tarea> findById(Long var1);

    public Tarea save(Tarea var1);

    public void deleteById(Long var1);
}
