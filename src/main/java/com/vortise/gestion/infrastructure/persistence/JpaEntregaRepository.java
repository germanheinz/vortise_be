/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Entrega;
import com.vortise.gestion.domain.repository.EntregaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEntregaRepository
extends JpaRepository<Entrega, Long>,
EntregaRepository {
    @Override
    public List<Entrega> findByProyectoId(Long var1);
}
