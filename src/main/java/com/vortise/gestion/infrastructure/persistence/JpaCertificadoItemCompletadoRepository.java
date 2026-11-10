package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.CertificadoItemCompletado;
import com.vortise.gestion.domain.repository.CertificadoItemCompletadoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCertificadoItemCompletadoRepository extends JpaRepository<CertificadoItemCompletado, Long>, CertificadoItemCompletadoRepository {
    @Override
    List<CertificadoItemCompletado> findByCertificadoProyectoId(Long proyectoId);

    @Override
    Optional<CertificadoItemCompletado> findByCertificadoIdAndRubroId(Long certificadoId, Long rubroId);
}
