package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.ReporteImagen;
import com.vortise.gestion.domain.repository.ReporteImagenRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReporteImagenRepository extends JpaRepository<ReporteImagen, Long>, ReporteImagenRepository {
    @Override
    List<ReporteImagen> findByProyectoIdAndReporteIsNullOrderByOrdenAsc(Long proyectoId);

    @Override
    List<ReporteImagen> findByReporteIdOrderByOrdenAsc(Long reporteId);

    @Override
    List<ReporteImagen> findByReporteIdIn(Collection<Long> reporteIds);

    @Override
    Optional<ReporteImagen> findByIdAndProyectoId(Long id, Long proyectoId);
}
