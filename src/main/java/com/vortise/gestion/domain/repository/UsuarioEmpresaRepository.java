package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.UsuarioEmpresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UsuarioEmpresaRepository {
    @EntityGraph(attributePaths = "empresa")
        @Query("select u from UsuarioEmpresa u where u.superAdmin = false and (:empresaInternaId is null or u.empresa.id <> :empresaInternaId) and (:empresaId is null or u.empresa.id = :empresaId) and (:activo is null or u.activo = :activo) and (:q = '' or lower(u.nombre) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')))")
        Page<UsuarioEmpresa> buscarSuperAdmin(@Param("empresaInternaId") Long empresaInternaId, @Param("empresaId") Long empresaId, @Param("activo") Boolean activo,
            @Param("q") String q, Pageable pageable);

        @Query("select count(u) from UsuarioEmpresa u where u.superAdmin = false and u.activo = true and (:empresaInternaId is null or u.empresa.id <> :empresaInternaId)")
        long contarActivosSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);
        @Query("select count(u) from UsuarioEmpresa u where u.superAdmin = false and u.activo = false and (:empresaInternaId is null or u.empresa.id <> :empresaInternaId)")
        long contarInactivosSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);
    long countByEmpresaIdAndActivoTrueAndSuperAdminFalse(Long empresaId);

    Optional<UsuarioEmpresa> findByEmail(String email);
    Optional<UsuarioEmpresa> findById(Long id);
    List<UsuarioEmpresa> findByEmpresaId(Long empresaId);
    long countByEmpresaId(Long empresaId);
    long countByEmpresaIdAndActivoTrue(Long empresaId);
    UsuarioEmpresa save(UsuarioEmpresa usuario);
    void deleteById(Long id);
}
