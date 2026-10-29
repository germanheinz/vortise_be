package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.InvitacionesDto.*;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.exception.*;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitacionesUseCase {
    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final InvitacionEquipoRepository invitaciones;
    private final CurrentIdentity identity;
    private final EquipoAccessPolicy policy;
    private final PasswordEncoder encoder;
    private final AutenticacionUseCase autenticacion;
    private final String frontend;
    public InvitacionesUseCase(UsuarioEmpresaRepository usuarios, EmpresaRepository empresas, InvitacionEquipoRepository invitaciones,
            CurrentIdentity identity, EquipoAccessPolicy policy, PasswordEncoder encoder, AutenticacionUseCase autenticacion,
            @Value("${app.frontend-url}") String frontend) {
        this.usuarios = usuarios; this.empresas = empresas; this.invitaciones = invitaciones;
        this.identity = identity; this.policy = policy; this.encoder = encoder; this.autenticacion = autenticacion;
        this.frontend = frontend.replaceAll("/+$", "");
    }
    @Transactional
    public Creada crear(Crear dto) {
        UsuarioEmpresa titular = titularBloqueado();
        Set<Long> ids = new TreeSet<>(dto.empresaIds());
        verificarEmpresas(titular.getId(), ids);
        verificarCupo(titular.getId());
        String token = InvitacionToken.nuevo();
        InvitacionEquipo invitacion = invitaciones.save(new InvitacionEquipo(InvitacionToken.hash(token), titular.getId(), ids, Instant.now()));
        return new Creada(invitacion.getId(), frontend + "/invitacion#token=" + token, invitacion.getVenceEn(), nombres(ids));
    }
    @Transactional(readOnly = true)
    public Equipo listar() {
        if (!identity.isAdministrator()) throw new AccessDeniedException("Solo el administrador gestiona invitaciones.");
        Long titular = identity.usuarioId();
        var propias = empresas.findByCreadoPorUsuarioId(titular);
        var cubiertas = policy.coberturasPro(titular, propias.stream().map(Empresa::getId).toList());
        var disponibles = propias.stream().filter(e -> cubiertas.containsKey(e.getId()))
            .map(e -> new EmpresaAcceso(e.getId(), e.getNombre())).toList();
        long activos = usuarios.countByInvitanteIdAndActivoTrue(titular);
        var lista = invitaciones.findByTitularIdOrderByIdDesc(titular).stream()
            .map(i -> new Resumen(i.getId(), i.getVenceEn(), i.estado(Instant.now()), nombres(i.getEmpresaIds()))).toList();
        return new Equipo(lista, activos, EquipoAccessPolicy.MAX_INVITADOS,
            !disponibles.isEmpty() && activos < EquipoAccessPolicy.MAX_INVITADOS, disponibles);
    }
    @Transactional
    public void revocar(Long id) {
        UsuarioEmpresa titular = titularBloqueado();
        InvitacionEquipo invitacion = invitaciones.bloquearPorId(id)
            .filter(i -> titular.getId().equals(i.getTitularId())).orElseThrow(this::noDisponible);
        if (invitacion.disponible(Instant.now())) invitacion.revocar(Instant.now());
    }
    @Transactional(readOnly = true)
    public Consulta consultar(String token) {
        InvitacionEquipo invitacion = buscarDisponible(token);
        verificarEmpresas(invitacion.getTitularId(), invitacion.getEmpresaIds());
        return new Consulta(nombres(invitacion.getEmpresaIds()), invitacion.getVenceEn());
    }
    @Transactional
    public LoginResponseDto aceptar(Aceptar dto) {
        // Same global owner lock for all invitations and user reactivations, then invitation lock.
        var candidato = invitaciones.buscarIdentidad(InvitacionToken.hash(dto.token())).orElseThrow(this::noDisponible);
        usuarios.bloquearPorId(candidato.getTitularId()).orElseThrow(this::noDisponible);
        InvitacionEquipo invitacion = invitaciones.bloquearPorId(candidato.getId()).orElseThrow(this::noDisponible);
        if (!invitacion.disponible(Instant.now())) throw noDisponible();
        verificarEmpresas(invitacion.getTitularId(), invitacion.getEmpresaIds());
        verificarCupo(invitacion.getTitularId());
        PasswordPolicy.validate(dto.password());
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmail(email).isPresent()) throw new ConflictoEstadoException("No se pudo crear la cuenta con ese email. Iniciá sesión con tu cuenta existente.");
        Long primera = invitacion.getEmpresaIds().stream().min(Long::compareTo).orElseThrow(this::noDisponible);
        Empresa empresa = empresas.findById(primera).orElseThrow(this::noDisponible);
        UsuarioEmpresa usuario = new UsuarioEmpresa(empresa, email, dto.nombre().trim(), encoder.encode(dto.password()), RolEmpresa.COLABORADOR);
        usuario.asignarEquipo(invitacion.getTitularId(), invitacion.getEmpresaIds());
        usuario = usuarios.save(usuario);
        invitacion.consumir(Instant.now());
        return autenticacion.sesion(usuario);
    }
    private UsuarioEmpresa titularBloqueado() {
        if (!identity.isAdministrator()) throw new AccessDeniedException("Solo el administrador gestiona invitaciones.");
        return usuarios.bloquearPorId(identity.usuarioId())
            .filter(u -> u.isActivo() && u.getRol() == RolEmpresa.ADMINISTRADOR).orElseThrow(this::noDisponible);
    }
    private InvitacionEquipo buscarDisponible(String token) {
        return invitaciones.findByTokenHash(InvitacionToken.hash(token)).filter(i -> i.disponible(Instant.now())).orElseThrow(this::noDisponible);
    }
    private void verificarEmpresas(Long titular, Set<Long> ids) {
        if (ids.isEmpty() || ids.size() > 100) throw new SolicitudInvalidaException("Seleccioná entre una y cien empresas.");
        if (ids.stream().anyMatch(Objects::isNull) || policy.coberturasPro(titular, ids).size() != ids.size()) {
            throw new AccessDeniedException("La invitación requiere empresas propias con cobertura Pro activa.");
        }
    }
    private void verificarCupo(Long titular) {
        if (usuarios.countByInvitanteIdAndActivoTrue(titular) >= EquipoAccessPolicy.MAX_INVITADOS) {
            throw new ConflictoEstadoException("Pro permite hasta cinco usuarios invitados activos.");
        }
    }
    private List<EmpresaAcceso> nombres(Set<Long> ids) {
        return ids.stream().sorted().flatMap(id -> empresas.findById(id).stream())
            .map(e -> new EmpresaAcceso(e.getId(), e.getNombre())).toList();
    }
    private SolicitudInvalidaException noDisponible() { return new SolicitudInvalidaException("La invitación no está disponible."); }
}
