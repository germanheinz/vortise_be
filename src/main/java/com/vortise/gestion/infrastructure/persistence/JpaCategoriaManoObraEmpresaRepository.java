package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCategoriaManoObraEmpresaRepository extends JpaRepository<CategoriaManoObraEmpresa, Long>, CategoriaManoObraEmpresaRepository { }
