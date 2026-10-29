package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Empresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmpresaRepository {
    Optional<Empresa> bloquearPorId(Long id);
    long count();

    @Query("select e from Empresa e where (:empresaInternaId is null or e.id <> :empresaInternaId) and (:q = '' or lower(e.nombre) like lower(concat('%', :q, '%')))")
    Page<Empresa> buscarSuperAdmin(@Param("empresaInternaId") Long empresaInternaId, @Param("q") String q, Pageable pageable);

    List<Empresa> findAll();
    List<Empresa> findByIdIn(java.util.Collection<Long> ids);

    Optional<Empresa> findById(Long id);

    Optional<Empresa> findByNombreIgnoreCase(String nombre);

    Optional<Empresa> findByIdentificacionFiscal(String identificacionFiscal);

    List<Empresa> findByCreadoPorUsuarioId(Long usuarioId);

    Empresa save(Empresa empresa);

    void deleteById(Long id);
}
