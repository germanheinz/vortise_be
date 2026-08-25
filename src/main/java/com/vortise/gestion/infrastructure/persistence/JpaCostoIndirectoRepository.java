/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.CostoIndirecto;
import com.vortise.gestion.domain.repository.CostoIndirectoRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCostoIndirectoRepository
extends JpaRepository<CostoIndirecto, Long>,
CostoIndirectoRepository {
    @Override
    public List<CostoIndirecto> findByProyectoId(Long var1);
}
