package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.ResponsableEmpresa;
import com.vortise.gestion.repository.ResponsableEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaResponsableEmpresaRepository extends JpaRepository<ResponsableEmpresa, Long>, ResponsableEmpresaRepository {
}
