package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.CatalogoEmpresa;
import java.util.List;
import java.util.Optional;

public interface CatalogoEmpresaRepository {
    List<CatalogoEmpresa> findByEmpresaId(Long empresaId);
    List<CatalogoEmpresa> findByEmpresaIdAndTipo(Long empresaId, String tipo);
    Optional<CatalogoEmpresa> findById(Long id);
    CatalogoEmpresa save(CatalogoEmpresa item);
    void deleteById(Long id);
}
