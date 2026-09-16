package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.UsuarioEmpresa;
import java.util.List;
import java.util.Optional;

public interface UsuarioEmpresaRepository {
    Optional<UsuarioEmpresa> findByEmail(String email);
    Optional<UsuarioEmpresa> findById(Long id);
    List<UsuarioEmpresa> findByEmpresaId(Long empresaId);
    long countByEmpresaId(Long empresaId);
    long countByEmpresaIdAndActivoTrue(Long empresaId);
    UsuarioEmpresa save(UsuarioEmpresa usuario);
    void deleteById(Long id);
}
