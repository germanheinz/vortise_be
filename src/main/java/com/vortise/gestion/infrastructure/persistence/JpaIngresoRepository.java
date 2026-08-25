/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.repository.IngresoRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaIngresoRepository
extends JpaRepository<Ingreso, Long>,
IngresoRepository {
    @Override
    public List<Ingreso> findByProyectoId(Long var1);
}
