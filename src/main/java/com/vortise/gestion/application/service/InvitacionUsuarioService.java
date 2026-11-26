package com.vortise.gestion.application.service;

import com.vortise.gestion.application.helper.InvitacionToken;
import com.vortise.gestion.application.helper.PasswordPolicy;
import com.vortise.gestion.application.audit.AuditLog;
import com.vortise.gestion.application.dto.InvitacionUsuarioDto.*;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.InvitacionUsuario;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.InvitacionUsuarioRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vortise.gestion.service.ClerkUserSyncService;

/**
 * An administrator invites a person to the company with a fixed role. The person opens a one-time link,
 * chooses their own name and password, and lands in the session with exactly that role (and sites).
 */
@Service
public class InvitacionUsuarioService {
    /** Roles an administrator can hand out; RESPONSABLE is not offered (it behaves like CONSULTA). */
    static final Set<RolEmpresa> ROLES_INVITABLES = Set.of(RolEmpresa.ADMINISTRADOR, RolEmpresa.JEFE_DE_OBRA, RolEmpresa.CONSULTA);

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final InvitacionUsuarioRepository invitaciones;
    private final ProyectoRepository proyectos;
    private final CurrentIdentity identity;
    private final PasswordEncoder encoder;
    private final AutenticacionService autenticacion;
    private final ClerkUserSyncService clerkSync;
    private final String frontend;

    public InvitacionUsuarioService(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios, InvitacionUsuarioRepository invitaciones,
            ProyectoRepository proyectos, CurrentIdentity identity, PasswordEncoder encoder, AutenticacionService autenticacion,
            ClerkUserSyncService clerkSync, @Value("${app.frontend-url}") String frontend) {
        this.empresas = empresas; this.usuarios = usuarios; this.invitaciones = invitaciones; this.proyectos = proyectos;
        this.identity = identity; this.encoder = encoder; this.autenticacion = autenticacion; this.clerkSync = clerkSync;
        this.frontend = frontend.replaceAll("/+$", "");
    }

    @Transactional
    public Creada crear(Long empresaId, Crear dto) {
        identity.requireEmpresa(empresaId);
        Empresa empresa = empresas.bloquearPorId(empresaId).orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        if (!ROLES_INVITABLES.contains(dto.rol())) throw new SolicitudInvalidaException("Ese rol no se puede asignar por invitación.");
        String email = normalizar(dto.email());
        if (usuarios.findByEmail(email).isPresent()) throw new ConflictoEstadoException("Ya existe un usuario con ese email.");
        Instant now = Instant.now();
        if (invitaciones.existePendiente(empresaId, email, now)) {
            throw new ConflictoEstadoException("Ya hay una invitación pendiente para ese email. Revocala para crear otra.");
        }
        verificarCupo(empresa, now);
        Set<Long> obras = obrasValidas(empresaId, dto);
        String token = InvitacionToken.nuevo();
        InvitacionUsuario invitacion = invitaciones.save(new InvitacionUsuario(InvitacionToken.hash(token), empresaId,
            identity.usuarioId(), email, dto.rol(), obras, now));
        AuditLog.userInvited(identity.usuarioId(), invitacion.getId(), dto.rol().name(), obras.size());
        return new Creada(invitacion.getId(), frontend + "/invitacion-usuario#codigo=" + token, invitacion.getVenceEn(), email, dto.rol(), obras.size());
    }

    @Transactional(readOnly = true)
    public List<Resumen> listar(Long empresaId) {
        identity.requireEmpresa(empresaId);
        Instant now = Instant.now();
        return invitaciones.findByEmpresaIdOrderByIdDesc(empresaId).stream()
            .map(i -> new Resumen(i.getId(), i.getEmail(), i.getRol(), i.getObraIds().size(), i.estado(now), i.getVenceEn())).toList();
    }

    @Transactional
    public void revocar(Long empresaId, Long id) {
        identity.requireEmpresa(empresaId);
        InvitacionUsuario invitacion = invitaciones.bloquearPorId(id).filter(i -> empresaId.equals(i.getEmpresaId()))
            .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));
        if (invitacion.disponible(Instant.now())) invitacion.revocar(Instant.now());
    }

    @Transactional(readOnly = true)
    public Consulta consultar(String token) {
        InvitacionUsuario invitacion = disponible(token);
        Empresa empresa = empresas.findById(invitacion.getEmpresaId()).orElseThrow(this::noDisponible);
        return new Consulta(empresa.getNombre(), invitacion.getEmail(), invitacion.getRol(), invitacion.getVenceEn());
    }

    @Transactional
    public LoginResponseDto aceptar(Aceptar dto) {
        Long empresaId = disponible(dto.token()).getEmpresaId();
        // Same lock order as creation: company first, then the invitation.
        Empresa empresa = empresas.bloquearPorId(empresaId).orElseThrow(this::noDisponible);
        InvitacionUsuario invitacion = invitaciones.bloquearPorId(disponible(dto.token()).getId()).orElseThrow(this::noDisponible);
        Instant now = Instant.now();
        if (!invitacion.disponible(now)) throw noDisponible();
        PasswordPolicy.validate(dto.password());
        if (usuarios.findByEmail(invitacion.getEmail()).isPresent()) {
            throw new ConflictoEstadoException("No se pudo crear la cuenta con ese email. Iniciá sesión con tu cuenta existente.");
        }
        if (usuarios.countByEmpresaIdAndActivoTrue(empresaId) >= empresa.getLimiteUsuarios()) {
            throw new ConflictoEstadoException("La empresa alcanzó su límite de usuarios. Pedile al administrador que libere una plaza.");
        }
        UsuarioEmpresa usuario = new UsuarioEmpresa(empresa, invitacion.getEmail(), dto.nombre().trim(), encoder.encode(dto.password()), invitacion.getRol());
        if (invitacion.getRol() == RolEmpresa.JEFE_DE_OBRA) usuario.asignarObras(obrasDeLaEmpresa(empresaId, invitacion.getObraIds()));
        usuario = usuarios.save(usuario);
        invitacion.consumir(now);
        AuditLog.userInvitationAccepted(usuario.getId(), invitacion.getId(), invitacion.getRol().name());
        return autenticacion.sesion(usuario);
    }

    @Transactional
    public LoginResponseDto aceptarClerk(AceptarClerk dto) {
        InvitacionUsuario invitacion = disponible(dto.codigo());
        Long empresaId = invitacion.getEmpresaId();
        Empresa empresa = empresas.bloquearPorId(empresaId).orElseThrow(this::noDisponible);
        invitacion = invitaciones.bloquearPorId(invitacion.getId()).orElseThrow(this::noDisponible);
        Instant now = Instant.now();
        if (!invitacion.disponible(now)) throw noDisponible();
        
        if (usuarios.countByEmpresaIdAndActivoTrue(empresaId) >= empresa.getLimiteUsuarios()) {
            throw new ConflictoEstadoException("La empresa alcanzó su límite de usuarios. Pedile al administrador que libere una plaza.");
        }
        
        UsuarioEmpresa usuario = clerkSync.sincronizarDesdeClerk(dto.clerkUserId(), invitacion.getEmail());
        if (usuario == null) {
            throw new SolicitudInvalidaException("No se pudo sincronizar el usuario con Clerk.");
        }
        
        if (invitacion.getRol() == RolEmpresa.JEFE_DE_OBRA) {
            usuario.asignarObras(obrasDeLaEmpresa(empresaId, invitacion.getObraIds()));
            usuarios.save(usuario);
        }
        
        invitacion.consumir(now);
        AuditLog.userInvitationAccepted(usuario.getId(), invitacion.getId(), invitacion.getRol().name());
        return autenticacion.sesion(usuario);
    }
    private void verificarCupo(Empresa empresa, Instant now) {
        long ocupadas = usuarios.countByEmpresaIdAndActivoTrue(empresa.getId()) + invitaciones.contarPendientes(empresa.getId(), now);
        if (ocupadas >= empresa.getLimiteUsuarios()) {
            throw new ConflictoEstadoException("Se alcanzó el límite de " + empresa.getLimiteUsuarios() + " usuarios para esta empresa (incluye invitaciones pendientes).");
        }
    }

    // Only a jefe de obra carries sites, and only sites of this company.
    private Set<Long> obrasValidas(Long empresaId, Crear dto) {
        Set<Long> pedidas = dto.obraIds() == null ? Set.of() : new HashSet<>(dto.obraIds());
        if (dto.rol() != RolEmpresa.JEFE_DE_OBRA) {
            if (!pedidas.isEmpty()) throw new SolicitudInvalidaException("Solo un jefe de obra tiene obras asignadas.");
            return Set.of();
        }
        for (Long obraId : pedidas) {
            boolean propia = proyectos.findById(obraId)
                .map(p -> p.getEmpresaRelacionada() != null && empresaId.equals(p.getEmpresaRelacionada().getId())).orElse(false);
            if (!propia) throw new SolicitudInvalidaException("Una de las obras no pertenece a la empresa.");
        }
        return pedidas;
    }

    // A site may have been deleted or moved since the invitation was created: keep only the ones that still belong.
    private Set<Long> obrasDeLaEmpresa(Long empresaId, Set<Long> ids) {
        Set<Long> vigentes = new HashSet<>();
        for (Long obraId : ids) {
            if (proyectos.findById(obraId).map(p -> p.getEmpresaRelacionada() != null && empresaId.equals(p.getEmpresaRelacionada().getId())).orElse(false)) {
                vigentes.add(obraId);
            }
        }
        return vigentes;
    }

    private InvitacionUsuario disponible(String token) {
        return invitaciones.findByTokenHash(InvitacionToken.hash(token)).filter(i -> i.disponible(Instant.now())).orElseThrow(this::noDisponible);
    }

    private static String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private SolicitudInvalidaException noDisponible() { return new SolicitudInvalidaException("La invitación no está disponible."); }
}
