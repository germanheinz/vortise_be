package com.vortise.gestion.service;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.mapper.EmpresaMapper;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import com.vortise.gestion.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.repository.UnidadMetricaEmpresaRepository;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpresaService {
    private final EmpresaRepository empresaRepository;
    private final CurrentIdentity currentTenant;
    private final EmpresaConfiguracionService configuracion;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final ResponsableEmpresaRepository responsableEmpresaRepository;
    private final UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository;
    private final CatalogoEmpresaRepository catalogoEmpresaRepository;
    private final CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository, CurrentIdentity currentTenant, EmpresaConfiguracionService configuracion,
            ProyectoRepository proyectoRepository, UsuarioEmpresaRepository usuarioEmpresaRepository, ResponsableEmpresaRepository responsableEmpresaRepository,
            UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository, CatalogoEmpresaRepository catalogoEmpresaRepository, CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository) {
        this.empresaRepository = empresaRepository;
        this.currentTenant = currentTenant;
        this.configuracion = configuracion;
        this.proyectoRepository = proyectoRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.responsableEmpresaRepository = responsableEmpresaRepository;
        this.unidadMetricaEmpresaRepository = unidadMetricaEmpresaRepository;
        this.catalogoEmpresaRepository = catalogoEmpresaRepository;
        this.categoriaManoObraEmpresaRepository = categoriaManoObraEmpresaRepository;
    }

    @Transactional
    public EmpresaDto create(EmpresaCreateDto dto) {
        ensureNameAvailable(dto.nombre(), null);
        ensureTaxIdAvailable(dto.identificacionFiscal(), null);
        Empresa empresa = new Empresa(dto.nombre().trim(), normalize(dto.identificacionFiscal()));
        empresa.setCreadoPorUsuarioId(currentTenant.usuarioId());
        Empresa saved = empresaRepository.save(empresa);
        configuracion.inicializar(saved.getId());
        return EmpresaMapper.toDto(saved);
    }

    @Transactional
    public EmpresaDto update(Long id, EmpresaCreateDto dto) {
        Empresa empresa = empresaRepository.bloquearPorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        currentTenant.requireEmpresa(id);
        ensureNameAvailable(dto.nombre(), id);
        ensureTaxIdAvailable(dto.identificacionFiscal(), id);
        empresa.setNombre(dto.nombre().trim());
        empresa.setIdentificacionFiscal(normalize(dto.identificacionFiscal()));
        return EmpresaMapper.toDto(empresaRepository.save(empresa));
    }

    private void ensureNameAvailable(String nombre, Long currentId) {
        empresaRepository.findByNombreIgnoreCase(nombre.trim())
            .filter(existing -> !existing.getId().equals(currentId))
            .ifPresent(existing -> {
                throw new SolicitudInvalidaException("Ya existe una empresa con ese nombre");
            });
    }

    private void ensureTaxIdAvailable(String identificacionFiscal, Long currentId) {
        String taxId = normalize(identificacionFiscal);
        if (taxId == null) {
            return;
        }

        empresaRepository.findByIdentificacionFiscal(taxId)
            .filter(existing -> !existing.getId().equals(currentId))
            .ifPresent(existing -> {
                throw new ConflictoEstadoException("Ya existe una empresa con esa identificación fiscal");
            });
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public List<EmpresaDto> listar() {
        if (currentTenant.isSuperAdmin()) {
            return empresaRepository.findAll().stream().map(EmpresaMapper::toDto).toList();
        }

        if (currentTenant.isCollaborator()) {
            return currentTenant.empresasAccesibles().stream().flatMap(id -> empresaRepository.findById(id).stream())
                .map(EmpresaMapper::toDto).toList();
        }
        Long ownId = currentTenant.empresaId();
        List<Empresa> empresas = new ArrayList<>(empresaRepository.findById(ownId).stream().toList());
        if (currentTenant.isAdministrator()) {
            empresaRepository.findByCreadoPorUsuarioId(currentTenant.usuarioId()).stream()
                .filter(empresa -> !empresa.getId().equals(ownId))
                .forEach(empresas::add);
        }
        return empresas.stream().map(EmpresaMapper::toDto).toList();
    }

    public EmpresaDto obtener(Long id) {
        return EmpresaMapper.toDto(empresaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada")));
    }

    @Transactional
    public void eliminar(Long id) {
        if (empresaRepository.findById(id).isEmpty()) {
            throw new RecursoNoEncontradoException("Empresa no encontrada");
        }
        if (proyectoRepository.countByEmpresaRelacionadaId(id) > 0) {
            throw new ConflictoEstadoException("No se puede eliminar una empresa que tiene obras asociadas");
        }

        var usuarios = usuarioEmpresaRepository.findByEmpresaId(id);
        if (usuarios.stream().anyMatch(usuario -> usuario.isColaborador()
                || !usuarioEmpresaRepository.findByInvitanteId(usuario.getId()).isEmpty())) {
            throw new ConflictoEstadoException("No se puede eliminar una empresa con accesos de equipo asignados.");
        }
        usuarioEmpresaRepository.findByEmpresaId(id).forEach(usuario -> usuarioEmpresaRepository.deleteById(usuario.getId()));
        responsableEmpresaRepository.findByEmpresaId(id).forEach(item -> responsableEmpresaRepository.deleteById(item.getId()));
        unidadMetricaEmpresaRepository.findByEmpresaId(id).forEach(item -> unidadMetricaEmpresaRepository.deleteById(item.getId()));
        catalogoEmpresaRepository.findByEmpresaId(id).forEach(item -> catalogoEmpresaRepository.deleteById(item.getId()));
        categoriaManoObraEmpresaRepository.findByEmpresaId(id).forEach(item -> categoriaManoObraEmpresaRepository.deleteById(item.getId()));

        empresaRepository.deleteById(id);
    }
}
