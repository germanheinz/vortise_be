package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long>, UsuarioEmpresaRepository { }
