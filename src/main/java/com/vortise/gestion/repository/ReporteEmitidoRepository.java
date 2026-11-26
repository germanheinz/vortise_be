package com.vortise.gestion.repository;

import com.vortise.gestion.model.ReporteEmitido;
import java.util.List;
import java.util.Optional;

public interface ReporteEmitidoRepository {
    List<ReporteEmitido> findByProyectoIdOrderByNumeroDesc(Long proyectoId);

    Optional<ReporteEmitido> findByIdAndProyectoId(Long id, Long proyectoId);

    int ultimoNumero(Long proyectoId);

    ReporteEmitido save(ReporteEmitido reporte);
}
