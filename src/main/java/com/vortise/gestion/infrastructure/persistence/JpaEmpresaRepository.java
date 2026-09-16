package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEmpresaRepository extends JpaRepository<Empresa, Long>, EmpresaRepository {
}
