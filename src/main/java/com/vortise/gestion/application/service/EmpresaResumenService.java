package com.vortise.gestion.application.service;

import com.vortise.gestion.application.mapper.ObraTareaMapper;
import com.vortise.gestion.application.dto.EmpresaResumenDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.repository.EmpresaConteoProjection;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmpresaResumenService {
    private static final double CUMPLIDA = 100.0;
    private final EmpresaRepository empresas;
    private final ProyectoRepository proyectos;
    private final RestriccionObraRepository restricciones;
    private final RegistroHorasRepository horas;
    private final ObraTareaRepository tareas;
    private final CurrentIdentity identity;

    public EmpresaResumenService(EmpresaRepository empresas, ProyectoRepository proyectos, RestriccionObraRepository restricciones,
            RegistroHorasRepository horas, ObraTareaRepository tareas, CurrentIdentity identity) {
        this.empresas = empresas; this.proyectos = proyectos; this.restricciones = restricciones;
        this.horas = horas; this.tareas = tareas; this.identity = identity;
    }

    public EmpresaResumenDto execute(Long empresaId) {
        identity.requireEmpresa(empresaId);
        if (empresas.findById(empresaId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
        Set<Long> ids = Set.of(empresaId);
        Double totalHoras = horas.sumHorasPorEmpresaRelacionada(empresaId);
        var vinculadas = tareas.findConActividadByEmpresaRelacionadaId(empresaId);
        // Same rule the task list shows: the persisted percentage is ignored when the task has an activity.
        long cumplidas = vinculadas.stream().map(ObraTareaMapper::toDto)
            .filter(dto -> dto.porcentajeCumplimiento() != null && dto.porcentajeCumplimiento() >= CUMPLIDA).count();
        return new EmpresaResumenDto(empresaId, count(proyectos.contarPorEmpresaRelacionada(ids), empresaId),
            count(restricciones.contarPorEmpresaRelacionada(ids), empresaId), totalHoras == null ? 0.0 : totalHoras,
            vinculadas.size(), cumplidas);
    }

    private static long count(List<EmpresaConteoProjection> rows, Long empresaId) {
        return rows.stream().filter(row -> empresaId.equals(row.getEmpresaId())).mapToLong(EmpresaConteoProjection::getCantidad).sum();
    }
}
