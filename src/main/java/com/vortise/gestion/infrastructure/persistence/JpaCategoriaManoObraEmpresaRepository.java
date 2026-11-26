package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.repository.CategoriaManoObraEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCategoriaManoObraEmpresaRepository extends JpaRepository<CategoriaManoObraEmpresa, Long>, CategoriaManoObraEmpresaRepository { }
