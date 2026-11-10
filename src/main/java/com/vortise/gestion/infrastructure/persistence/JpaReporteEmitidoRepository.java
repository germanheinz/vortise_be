package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.ReporteEmitido;
import com.vortise.gestion.domain.repository.ReporteEmitidoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReporteEmitidoRepository extends JpaRepository<ReporteEmitido, Long>, ReporteEmitidoRepository {
    @Override
    List<ReporteEmitido> findByProyectoIdOrderByNumeroDesc(Long proyectoId);

    @Override
    Optional<ReporteEmitido> findByIdAndProyectoId(Long id, Long proyectoId);

    @Override
    @Query("select coalesce(max(r.numero), 0) from ReporteEmitido r where r.proyecto.id = :proyectoId")
    int ultimoNumero(@Param("proyectoId") Long proyectoId);
}
