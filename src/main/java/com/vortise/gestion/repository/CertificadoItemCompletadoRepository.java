package com.vortise.gestion.repository;

import com.vortise.gestion.model.CertificadoItemCompletado;
import java.util.List;
import java.util.Optional;

public interface CertificadoItemCompletadoRepository {
    List<CertificadoItemCompletado> findByCertificadoProyectoId(Long proyectoId);

    Optional<CertificadoItemCompletado> findByCertificadoIdAndRubroId(Long certificadoId, Long rubroId);

    CertificadoItemCompletado save(CertificadoItemCompletado item);

    void delete(CertificadoItemCompletado item);
}
