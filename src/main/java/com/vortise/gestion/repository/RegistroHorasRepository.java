package com.vortise.gestion.repository;

import com.vortise.gestion.model.RegistroHoras;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistroHorasRepository {
    @Query("select h.proyecto.empresaRelacionada.id as empresaId, count(h) as cantidad from RegistroHoras h where h.proyecto.empresaRelacionada.id in :empresaIds group by h.proyecto.empresaRelacionada.id")
    List<EmpresaConteoProjection> contarPorEmpresaRelacionada(@Param("empresaIds") Collection<Long> empresaIds);

    @Query("select coalesce(sum(h.horas), 0.0) from RegistroHoras h where h.proyecto.empresaRelacionada.id = :empresaId")
    Double sumHorasPorEmpresaRelacionada(@Param("empresaId") Long empresaId);

    List<RegistroHoras> findAll();

    List<RegistroHoras> findByProyectoId(Long proyectoId);

    Optional<Long> findProyectoIdById(Long id);

    boolean existsByProyectoId(Long proyectoId);

    Double sumHorasByProyectoId(Long proyectoId);

    Optional<RegistroHoras> findById(Long id);

    RegistroHoras save(RegistroHoras registroHoras);

    void flush();

    void deleteById(Long id);
}