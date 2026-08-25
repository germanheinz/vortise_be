/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.repository.PlantaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPlantaRepository
extends JpaRepository<Planta, Long>,
PlantaRepository {
    @Override
    public List<Planta> findByProyectoId(Long var1);
}
