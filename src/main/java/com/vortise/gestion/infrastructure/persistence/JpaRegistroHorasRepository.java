package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRegistroHorasRepository extends JpaRepository<RegistroHoras, Long>, RegistroHorasRepository {
    @Override
    List<RegistroHoras> findAll();

    @Override
    List<RegistroHoras> findByProyectoId(Long proyectoId);
}