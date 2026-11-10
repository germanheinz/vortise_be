package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.CertificadoObra;
import com.vortise.gestion.domain.repository.CertificadoObraRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCertificadoObraRepository extends JpaRepository<CertificadoObra, Long>, CertificadoObraRepository {
    @Override
    List<CertificadoObra> findByProyectoIdOrderByFechaCorteAscIdAsc(Long proyectoId);

    @Override
    Optional<CertificadoObra> findByIdAndProyectoId(Long id, Long proyectoId);
}
