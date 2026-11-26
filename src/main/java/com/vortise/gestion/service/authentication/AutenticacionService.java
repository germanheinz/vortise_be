package com.vortise.gestion.service.authentication;

import com.vortise.gestion.application.exception.OperacionNoPermitidaException;
import com.vortise.gestion.service.EquipoAccessPolicy;
import com.vortise.gestion.service.pagos.SuscripcionConsultaService;
import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.MeDto;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.Suscripcion;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionService {
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SuscripcionConsultaService suscripciones;
    private final CurrentIdentity identidad;
    private final EquipoAccessPolicy equipo;
    private final LoginThrottle throttle;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AutenticacionService.class);

    public AutenticacionService(UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            SuscripcionConsultaService suscripciones, CurrentIdentity identidad, EquipoAccessPolicy equipo, LoginThrottle throttle) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.suscripciones = suscripciones;
        this.identidad = identidad; this.equipo = equipo; this.throttle = throttle;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request, String clientAddress) {
        String email = request.email().trim().toLowerCase();
        if (throttle.bloqueado(email, clientAddress)) {
            log.warn("Login blocked by rate limit");
            throw new com.vortise.gestion.application.exception.DemasiadosIntentosException();
        }
        UsuarioEmpresa usuario = usuarioRepository.findByEmail(email)
            .filter(item -> item.isActivo())
            .filter(item -> passwordEncoder.matches(request.password(), item.getPasswordHash()))
            .orElse(null);
        if (usuario == null) {
            registrarFallo(email, clientAddress);
            throw new org.springframework.security.authentication.BadCredentialsException("Email o contraseña incorrectos");
        }
        throttle.limpiarCuenta(email);
        usuario.setUltimoLoginExitosoEn(Instant.now());
        usuarioRepository.save(usuario);
        return sesion(usuario);
    }

    // A storage failure while counting must not turn a wrong password into a server error.
    private void registrarFallo(String email, String clientAddress) {
        try {
            throttle.registrarFallo(email, clientAddress);
            log.warn("Failed login attempt recorded");
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("Failed login attempt could not be recorded: {}", ex.getClass().getSimpleName());
        }
    }

    @Transactional(readOnly = true)
    public LoginResponseDto sesion(UsuarioEmpresa usuario) {
        equipo.requireAcceso(usuario);
        ContextoSesion contexto = contexto(usuario);
        var empresa = contexto.empresa();
        // Only a JEFE_DE_OBRA carries sites in the token; the claim is the source the interceptor checks.
        return new LoginResponseDto(jwtService.generate(usuario.getId(), empresa.getId(), usuario.getEmail(), usuario.getRol().name(), usuario.isSuperAdmin(), usuario.getSessionVersion(), contexto.obras()),
            usuario.getId(), empresa.getId(), empresa.getNombre(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(),
            usuario.isSuperAdmin(), contexto.plan().isPresent(), contexto.codigoPlan(), false, contexto.obras());
    }

    @Transactional(readOnly = true)
    public MeDto me() {
        var usuario = usuarioRepository.findById(identidad.usuarioId())
            .filter(UsuarioEmpresa::isActivo)
            .orElseThrow(() -> new OperacionNoPermitidaException("Usuario no disponible"));
        ContextoSesion contexto = contexto(usuario);
        return new MeDto(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(),
            contexto.empresa().getId(), contexto.empresa().getNombre(), usuario.isSuperAdmin(), contexto.plan().isPresent(),
            contexto.codigoPlan(), contexto.plan().map(Suscripcion::getTitular).orElse(null), false, contexto.obras());
    }

    private record ContextoSesion(Empresa empresa, Optional<Suscripcion> plan, java.util.List<Long> obras) {
        String codigoPlan() { return plan.map(s -> s.getPlan().getCodigo()).orElse(null); }
    }

    private ContextoSesion contexto(UsuarioEmpresa usuario) {
        java.util.List<Long> obras = usuario.getRol() == RolEmpresa.JEFE_DE_OBRA
            ? usuario.getObrasAsignadas().stream().sorted().toList() : java.util.List.of();
        return new ContextoSesion(usuario.isColaborador() ? equipo.empresaInicial(usuario) : usuario.getEmpresa(),
            suscripciones.planEfectivo(usuario), obras);
    }
}
