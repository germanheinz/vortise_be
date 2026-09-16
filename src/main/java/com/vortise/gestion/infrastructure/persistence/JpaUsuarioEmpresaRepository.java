package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long>, UsuarioEmpresaRepository { }
