package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RegistroHoras;
import java.util.List;
import java.util.Optional;

public interface RegistroHorasRepository {
    List<RegistroHoras> findAll();

    List<RegistroHoras> findByProyectoId(Long proyectoId);

    Optional<RegistroHoras> findById(Long id);

    RegistroHoras save(RegistroHoras registroHoras);

    void deleteById(Long id);
}