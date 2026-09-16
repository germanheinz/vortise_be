package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import java.util.List;
import java.util.Optional;

public interface CategoriaManoObraEmpresaRepository {
    List<CategoriaManoObraEmpresa> findByEmpresaId(Long empresaId);
    Optional<CategoriaManoObraEmpresa> findById(Long id);
    CategoriaManoObraEmpresa save(CategoriaManoObraEmpresa categoria);
    void deleteById(Long id);
}
