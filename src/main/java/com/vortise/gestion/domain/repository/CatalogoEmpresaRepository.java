package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.CatalogoEmpresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogoEmpresaRepository {
    List<CatalogoEmpresa> findByEmpresaId(Long empresaId);
    List<CatalogoEmpresa> findByEmpresaIdAndTipo(Long empresaId, String tipo);
    @Query("select case when count(p) > 0 then true else false end from Proyecto p join p.subcontratistas s where s.id = :catalogoId")
    boolean existsInSelectedProjects(@Param("catalogoId") Long catalogoId);
    @Query("select case when count(r) > 0 then true else false end from RubroObra r where r.subcontratista.id = :catalogoId")
    boolean existsAssignedToActivity(@Param("catalogoId") Long catalogoId);
    Optional<CatalogoEmpresa> findById(Long id);
    CatalogoEmpresa save(CatalogoEmpresa item);
    void deleteById(Long id);
}
