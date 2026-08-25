/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Tarea;
import com.vortise.gestion.domain.repository.TareaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTareaRepository
extends JpaRepository<Tarea, Long>,
TareaRepository {
    @Override
    public List<Tarea> findByEntregaId(Long var1);

    @Override
    public List<Tarea> findByEntregaProyectoId(Long var1);
}
