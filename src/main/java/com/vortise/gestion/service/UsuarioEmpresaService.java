package com.vortise.gestion.service;

import com.vortise.gestion.application.exception.OperacionNoPermitidaException;
import com.vortise.gestion.service.pagos.SuscripcionConsultaService;
import com.vortise.gestion.application.helper.PasswordPolicy;
import com.vortise.gestion.application.dto.UsuarioEmpresaDto;
import com.vortise.gestion.application.dto.UsuarioEmpresaUpdateDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioEmpresaService {
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuscripcionConsultaService suscripciones;
    private final EquipoAccessPolicy equipo;
    private final com.vortise.gestion.application.port.CurrentIdentity identity;
    private final com.vortise.gestion.repository.ProyectoRepository proyectos;

    @org.springframework.beans.factory.annotation.Autowired
    public UsuarioEmpresaService(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder,
            SuscripcionConsultaService suscripciones, EquipoAccessPolicy equipo, com.vortise.gestion.application.port.CurrentIdentity identity,
            com.vortise.gestion.repository.ProyectoRepository proyectos) {
        this.empresaRepository = empresaRepository; this.usuarioRepository = usuarioRepository; this.passwordEncoder = passwordEncoder;
        this.suscripciones = suscripciones; this.equipo = equipo; this.identity = identity; this.proyectos = proyectos;
    }

    public UsuarioEmpresaService(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder,
            SuscripcionConsultaService suscripciones, EquipoAccessPolicy equipo, com.vortise.gestion.application.port.CurrentIdentity identity) {
        this(empresaRepository, usuarioRepository, passwordEncoder, suscripciones, equipo, identity, null);
    }

    @Transactional(readOnly = true)
    public List<UsuarioEmpresaDto> getAll(Long empresaId) { requireEmpresa(empresaId); return usuarioRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList(); }
    @Transactional
    public UsuarioEmpresaDto create(Long empresaId, UsuarioEmpresaDto dto) {
        Empresa empresa = lockEmpresa(empresaId);
        if (dto.rol() == com.vortise.gestion.model.RolEmpresa.COLABORADOR) throw new SolicitudInvalidaException("Usá una invitación para crear colaboradores.");
        PasswordPolicy.validate(dto.password());
        usuarioRepository.findByEmail(dto.email().trim().toLowerCase(java.util.Locale.ROOT)).ifPresent(existing -> { throw new SolicitudInvalidaException("Ya existe un usuario con ese email"); });
        long activos = usuarioRepository.countByEmpresaIdAndActivoTrue(empresaId);
        if (activos >= empresa.getLimiteUsuarios()) {
            throw new ConflictoEstadoException("Se alcanzó el límite de " + empresa.getLimiteUsuarios() + " usuarios para esta empresa");
        }
        return toDto(usuarioRepository.save(new UsuarioEmpresa(empresa, dto.email().trim().toLowerCase(java.util.Locale.ROOT), dto.nombre().trim(), passwordEncoder.encode(dto.password()), dto.rol())));
    }
    @Transactional
    public UsuarioEmpresaDto update(Long empresaId, Long id, UsuarioEmpresaUpdateDto dto) {
        UsuarioEmpresa usuario = findOwned(empresaId, id);
        if (usuario.isColaborador()) {
            requireInvitante(usuario);
            usuarioRepository.bloquearPorId(usuario.getInvitanteId()).orElseThrow(() -> new RecursoNoEncontradoException("Titular no encontrado"));
            if (dto.rol() != com.vortise.gestion.model.RolEmpresa.COLABORADOR) {
                throw new SolicitudInvalidaException("El rol de un invitado no se puede cambiar.");
            }
            if (dto.activo() && !usuario.isActivo()) {
                if (usuario.getEmpresasAsignadas().stream().noneMatch(company -> equipo.coberturaPro(usuario.getInvitanteId(), company).isPresent())) {
                    throw new OperacionNoPermitidaException("La reactivación requiere Pro activo.");
                }
                if (usuarioRepository.countByInvitanteIdAndActivoTrue(usuario.getInvitanteId()) >= EquipoAccessPolicy.MAX_INVITADOS) {
                    throw new ConflictoEstadoException("Pro permite hasta cinco usuarios invitados activos.");
                }
            }
        } else if (dto.rol() == com.vortise.gestion.model.RolEmpresa.COLABORADOR) {
            throw new SolicitudInvalidaException("El rol colaborador se asigna mediante invitación.");
        }
        Empresa empresa = lockEmpresa(empresaId);
        if (!usuario.isColaborador() && dto.activo() && !usuario.isActivo() && usuarioRepository.countByEmpresaIdAndActivoTrue(empresaId) >= empresa.getLimiteUsuarios()) {
            throw new ConflictoEstadoException("Se alcanzó el límite de " + empresa.getLimiteUsuarios() + " usuarios para esta empresa");
        }
        if ((id.equals(identity.usuarioId()) || id.equals(empresa.getCreadoPorUsuarioId())) && (!dto.activo() || dto.rol() != usuario.getRol())) {
            throw new SolicitudInvalidaException("No podés quitar tu propio acceso de administrador.");
        }
        if (dto.password() != null && !dto.password().isBlank()) PasswordPolicy.validate(dto.password());
        usuario.setNombre(dto.nombre().trim()); usuario.setRol(dto.rol()); usuario.setActivo(dto.activo());
        applyObras(usuario, empresaId, dto);
        if (dto.password() != null && !dto.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        }
        UsuarioEmpresa saved = usuarioRepository.save(usuario);
        com.vortise.gestion.application.audit.AuditLog.userAccessChanged(identity.usuarioId(), saved.getId(), saved.getRol().name(),
            saved.getObrasAsignadas().size(), saved.isActivo());
        return toDto(saved);
    }
    @Transactional
    public void delete(Long empresaId, Long id) { requireEmpresa(empresaId); UsuarioEmpresa usuario = findOwned(empresaId, id); if (usuario.isColaborador()) { requireInvitante(usuario); usuarioRepository.bloquearPorId(usuario.getInvitanteId()).orElseThrow(() -> new RecursoNoEncontradoException("Titular no encontrado")); } if (id.equals(identity.usuarioId()) || id.equals(requireEmpresa(empresaId).getCreadoPorUsuarioId())) throw new SolicitudInvalidaException("No podés desactivar tu propio acceso."); usuario.setActivo(false); usuarioRepository.save(usuario); }
    private Empresa lockEmpresa(Long id) {
        return empresaRepository.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
    }
    // Only a JEFE_DE_OBRA has assigned sites, and only sites of the same company; any change revokes its sessions.
    private void applyObras(UsuarioEmpresa usuario, Long empresaId, UsuarioEmpresaUpdateDto dto) {
        if (dto.rol() != com.vortise.gestion.model.RolEmpresa.JEFE_DE_OBRA) {
            if (dto.obrasAsignadas() != null && !dto.obrasAsignadas().isEmpty()) {
                throw new SolicitudInvalidaException("Solo un jefe de obra tiene obras asignadas.");
            }
            usuario.asignarObras(java.util.Set.of());
            return;
        }
        if (dto.obrasAsignadas() == null) return;
        java.util.Set<Long> ids = new java.util.HashSet<>(dto.obrasAsignadas());
        for (Long obraId : ids) {
            boolean propia = obraId != null && proyectos != null && proyectos.findById(obraId)
                .map(p -> p.getEmpresaRelacionada() != null && empresaId.equals(p.getEmpresaRelacionada().getId())).orElse(false);
            if (!propia) throw new SolicitudInvalidaException("Una de las obras no pertenece a la empresa.");
        }
        usuario.asignarObras(ids);
    }
    private Empresa requireEmpresa(Long id) { return empresaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada")); }
    private UsuarioEmpresa findOwned(Long empresaId, Long id) { UsuarioEmpresa usuario = usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado")); if (!usuario.getEmpresa().getId().equals(empresaId) && !usuario.getEmpresasAsignadas().contains(empresaId)) throw new SolicitudInvalidaException("El usuario no pertenece a la empresa"); if (usuario.isSuperAdmin() && !identity.isSuperAdmin()) throw new OperacionNoPermitidaException("No podés administrar a un administrador de la plataforma."); return usuario; }
    private void requireInvitante(UsuarioEmpresa usuario) {
        if (!identity.usuarioId().equals(usuario.getInvitanteId())) throw new OperacionNoPermitidaException("Solo el titular puede gestionar este invitado.");
    }
    private UsuarioEmpresaDto toDto(UsuarioEmpresa u) {
        java.util.List<Long> obras = u.getRol() == com.vortise.gestion.model.RolEmpresa.JEFE_DE_OBRA
            ? u.getObrasAsignadas().stream().sorted().toList() : java.util.List.of();
        return new UsuarioEmpresaDto(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.isActivo(), null, suscripciones.resumenUsuario(u), obras);
    }
}
