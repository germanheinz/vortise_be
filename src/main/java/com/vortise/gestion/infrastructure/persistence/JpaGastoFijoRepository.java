/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaGastoFijoRepository
extends JpaRepository<GastoFijo, Long>,
GastoFijoRepository {
    @Override
    public List<GastoFijo> findByMes(Integer var1);
}
