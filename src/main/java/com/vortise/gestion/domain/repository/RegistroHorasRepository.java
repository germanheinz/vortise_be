package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RegistroHoras;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroHorasRepository {
    Optional<Long> findProyectoIdById(Long id);
    double sumHorasByProyectoId(Long proyectoId);
    boolean existsByProyectoId(Long proyectoId);
    void flush();
    @Query("select h.proyecto.empresaRelacionada.id as empresaId, count(h) as cantidad from RegistroHoras h where h.proyecto.empresaRelacionada.id in :empresaIds group by h.proyecto.empresaRelacionada.id")
    List<EmpresaConteoProjection> contarPorEmpresaRelacionada(@Param("empresaIds") Collection<Long> empresaIds);

    List<RegistroHoras> findAll();

    List<RegistroHoras> findByProyectoId(Long proyectoId);

    Optional<RegistroHoras> findById(Long id);

    RegistroHoras save(RegistroHoras registroHoras);

    void deleteById(Long id);
}