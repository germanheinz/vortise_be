package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.CertificadoItemCompletado;
import java.util.List;
import java.util.Optional;

public interface CertificadoItemCompletadoRepository {
    List<CertificadoItemCompletado> findByCertificadoProyectoId(Long proyectoId);

    Optional<CertificadoItemCompletado> findByCertificadoIdAndRubroId(Long certificadoId, Long rubroId);

    CertificadoItemCompletado save(CertificadoItemCompletado item);

    void delete(CertificadoItemCompletado item);
}
