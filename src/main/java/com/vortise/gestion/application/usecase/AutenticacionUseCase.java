package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.port.FotoPerfilStorage;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionUseCase {
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SuscripcionConsultaService suscripciones;
    private final FotoPerfilStorage fotos;
    private final EquipoAccessPolicy equipo;
    private final LoginThrottle throttle;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AutenticacionUseCase.class);

    public AutenticacionUseCase(UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            SuscripcionConsultaService suscripciones, FotoPerfilStorage fotos, EquipoAccessPolicy equipo, LoginThrottle throttle) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.suscripciones = suscripciones;
        this.fotos = fotos; this.equipo = equipo; this.throttle = throttle;
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
        Optional<Suscripcion> plan = suscripciones.planEfectivo(usuario);
        var empresa = usuario.isColaborador() ? equipo.empresaInicial(usuario) : usuario.getEmpresa();
        // Only a JEFE_DE_OBRA carries sites in the token; the claim is the source the interceptor checks.
        java.util.List<Long> obras = usuario.getRol() == com.vortise.gestion.domain.model.RolEmpresa.JEFE_DE_OBRA
            ? usuario.getObrasAsignadas().stream().sorted().toList() : java.util.List.of();
        return new LoginResponseDto(jwtService.generate(usuario.getId(), empresa.getId(), usuario.getEmail(), usuario.getRol().name(), usuario.isSuperAdmin(), usuario.getSessionVersion(), obras),
            usuario.getId(), empresa.getId(), empresa.getNombre(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(),
            usuario.isSuperAdmin(), plan.isPresent(), plan.map(s -> s.getPlan().getCodigo()).orElse(null), fotos.existe(usuario.getId()), obras);
    }
}
