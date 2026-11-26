/*
 * Decompiled with CFR 0.152.
 */
package com.vortise.gestion.repository;

import com.vortise.gestion.model.CostoIndirecto;
import java.util.List;
import java.util.Optional;

public interface CostoIndirectoRepository {
    public List<CostoIndirecto> findByProyectoId(Long var1);

    public Optional<CostoIndirecto> findById(Long var1);

    public CostoIndirecto save(CostoIndirecto var1);

    public void deleteById(Long var1);
}
