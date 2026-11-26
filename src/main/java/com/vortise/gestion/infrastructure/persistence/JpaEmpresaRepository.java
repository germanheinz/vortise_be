package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.repository.EmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEmpresaRepository extends JpaRepository<Empresa, Long>, EmpresaRepository, EmpresaLockRepository {


}
