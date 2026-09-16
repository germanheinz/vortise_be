package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.UnidadMetricaEmpresa;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUnidadMetricaEmpresaRepository extends JpaRepository<UnidadMetricaEmpresa, Long>, UnidadMetricaEmpresaRepository {
}
