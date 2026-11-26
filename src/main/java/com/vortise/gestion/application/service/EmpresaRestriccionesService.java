package com.vortise.gestion.application.service;

import com.vortise.gestion.application.mapper.RestriccionMapper;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmpresaRestriccionesService {
    private final EmpresaRepository empresas;
    private final RestriccionObraRepository restricciones;
    private final CurrentIdentity identity;

    public EmpresaRestriccionesService(EmpresaRepository empresas, RestriccionObraRepository restricciones, CurrentIdentity identity) {
        this.empresas = empresas; this.restricciones = restricciones; this.identity = identity;
    }

    public List<RestriccionDto> execute(Long empresaId) {
        identity.requireEmpresa(empresaId);
        if (empresas.findById(empresaId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
        return restricciones.findByProyectoEmpresaRelacionadaIdOrderByIdAsc(empresaId).stream().map(RestriccionMapper::toDto).toList();
    }
}
