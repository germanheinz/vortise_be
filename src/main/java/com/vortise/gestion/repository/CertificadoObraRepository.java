package com.vortise.gestion.repository;

import com.vortise.gestion.model.CertificadoObra;
import java.util.List;
import java.util.Optional;

public interface CertificadoObraRepository {
    List<CertificadoObra> findByProyectoIdOrderByFechaCorteAscIdAsc(Long proyectoId);

    Optional<CertificadoObra> findByIdAndProyectoId(Long id, Long proyectoId);

    CertificadoObra save(CertificadoObra certificado);
}
