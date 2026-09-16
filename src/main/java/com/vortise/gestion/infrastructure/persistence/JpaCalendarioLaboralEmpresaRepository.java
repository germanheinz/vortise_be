package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCalendarioLaboralEmpresaRepository extends JpaRepository<CalendarioLaboralEmpresa, Long>, CalendarioLaboralEmpresaRepository { }
