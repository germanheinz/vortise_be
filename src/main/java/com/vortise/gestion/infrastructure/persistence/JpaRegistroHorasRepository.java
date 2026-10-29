package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRegistroHorasRepository extends JpaRepository<RegistroHoras, Long>, RegistroHorasRepository {
    @Override
    @Query("select h.proyecto.id from RegistroHoras h where h.id = :id")
    java.util.Optional<Long> findProyectoIdById(@Param("id") Long id);

    @Override
    @Query("select coalesce(sum(h.horas), 0) from RegistroHoras h where h.proyecto.id = :proyectoId")
    double sumHorasByProyectoId(@Param("proyectoId") Long proyectoId);

    @Override
    List<RegistroHoras> findAll();

    @Override
    List<RegistroHoras> findByProyectoId(Long proyectoId);
}