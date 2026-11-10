package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.ReporteImagen;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReporteImagenRepository {
    List<ReporteImagen> findByProyectoIdAndReporteIsNullOrderByOrdenAsc(Long proyectoId);

    List<ReporteImagen> findByReporteIdOrderByOrdenAsc(Long reporteId);

    List<ReporteImagen> findByReporteIdIn(Collection<Long> reporteIds);

    Optional<ReporteImagen> findByIdAndProyectoId(Long id, Long proyectoId);

    ReporteImagen save(ReporteImagen imagen);

    void delete(ReporteImagen imagen);
}
