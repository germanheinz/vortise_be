/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPresupuestoLineaRepository
extends JpaRepository<PresupuestoLinea, Long>,
PresupuestoLineaRepository {
    @Override
    public List<PresupuestoLinea> findByProyectoId(Long var1);
}
