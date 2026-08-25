/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.GastoFijo;
import java.util.List;
import java.util.Optional;

public interface GastoFijoRepository {
    public List<GastoFijo> findAll();

    public List<GastoFijo> findByMes(Integer var1);

    public Optional<GastoFijo> findById(Long var1);

    public GastoFijo save(GastoFijo var1);

    public void deleteById(Long var1);
}
