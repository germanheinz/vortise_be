package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.UnidadMetricaEmpresa;
import java.util.List;
import java.util.Optional;

public interface UnidadMetricaEmpresaRepository {
    List<UnidadMetricaEmpresa> findByEmpresaId(Long empresaId);
    Optional<UnidadMetricaEmpresa> findById(Long id);
    UnidadMetricaEmpresa save(UnidadMetricaEmpresa unidad);
    void deleteById(Long id);
}
