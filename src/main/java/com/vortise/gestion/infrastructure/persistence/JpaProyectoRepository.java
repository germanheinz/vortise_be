/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProyectoRepository
extends JpaRepository<Proyecto, Long>,
ProyectoRepository {
}
