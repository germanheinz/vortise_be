package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.CatalogoEmpresa;
import com.vortise.gestion.repository.CatalogoEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCatalogoEmpresaRepository extends JpaRepository<CatalogoEmpresa, Long>, CatalogoEmpresaRepository { }
