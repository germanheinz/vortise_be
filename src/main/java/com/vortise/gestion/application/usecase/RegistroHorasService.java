package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serializes mutations of a project's hour ledger before deriving its total. */
@Service
@Transactional
public class RegistroHorasService {
    private final CurrentIdentity identity;
    private final ProyectoRepository projects;
    private final RegistroHorasRepository records;

    public RegistroHorasService(ProyectoRepository projects, RegistroHorasRepository records, CurrentIdentity identity) {
        this.identity = identity;
        this.projects = projects;
        this.records = records;
    }

    public RegistroHorasDto create(Long projectId, RegistroHorasDatos input) {
        Proyecto project = lockProject(projectId);
        RegistroHoras record = new RegistroHoras();
        record.setProyecto(project);
        RegistroHorasRules.apply(input, record, project);
        RegistroHoras saved = records.save(record);
        updateTotal(project);
        return RegistroHorasMapper.toDto(saved);
    }

    public RegistroHorasDto update(Long id, RegistroHorasDatos input) {
        Proyecto project = lockRecordProject(id);
        RegistroHoras record = records.findById(id).orElseThrow(RegistroHorasService::missingRecord);
        record.setProyecto(project);
        RegistroHorasRules.apply(input, record, project);
        RegistroHoras saved = records.save(record);
        updateTotal(project);
        return RegistroHorasMapper.toDto(saved);
    }

    public void delete(Long id) {
        Proyecto project = lockRecordProject(id);
        RegistroHoras record = records.findById(id).orElseThrow(RegistroHorasService::missingRecord);
        records.deleteById(record.getId());
        updateTotal(project);
    }

    private Proyecto lockRecordProject(Long id) {
        // A scalar projection avoids caching a stale entity before the project lock.
        Long projectId = records.findProyectoIdById(id).orElseThrow(RegistroHorasService::missingRecord);
        return lockProject(projectId);
    }

    private Proyecto lockProject(Long id) {
        Proyecto project = projects.bloquearPorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        if (project.getEmpresaRelacionada() == null || project.getEmpresaRelacionada().getId() == null) throw new AccessDeniedException("Recurso sin empresa asignada");
        identity.requireEmpresa(project.getEmpresaRelacionada().getId());
        return project;
    }

    private void updateTotal(Proyecto project) {
        records.flush();
        project.setHorasReales(RegistroHorasRules.roundHours(records.sumHorasByProyectoId(project.getId())));
        projects.save(project);
    }

    private static RecursoNoEncontradoException missingRecord() {
        return new RecursoNoEncontradoException("Registro de horas no encontrado");
    }
}
