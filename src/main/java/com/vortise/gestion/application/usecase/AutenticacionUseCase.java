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

    public AutenticacionUseCase(UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            SuscripcionConsultaService suscripciones, FotoPerfilStorage fotos, EquipoAccessPolicy equipo) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.suscripciones = suscripciones;
        this.fotos = fotos; this.equipo = equipo;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        UsuarioEmpresa usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
            .filter(item -> item.isActivo())
            .filter(item -> passwordEncoder.matches(request.password(), item.getPasswordHash()))
            .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Email o contraseña incorrectos"));
        usuario.setUltimoLoginExitosoEn(Instant.now());
        usuarioRepository.save(usuario);
        return sesion(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto sesion(UsuarioEmpresa usuario) {
        equipo.requireAcceso(usuario);
        Optional<Suscripcion> plan = suscripciones.planEfectivo(usuario);
        var empresa = usuario.isColaborador() ? equipo.empresaInicial(usuario) : usuario.getEmpresa();
        return new LoginResponseDto(jwtService.generate(usuario.getId(), empresa.getId(), usuario.getEmail(), usuario.getRol().name(), usuario.isSuperAdmin(), usuario.getSessionVersion()),
            usuario.getId(), empresa.getId(), empresa.getNombre(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(),
            usuario.isSuperAdmin(), plan.isPresent(), plan.map(s -> s.getPlan().getCodigo()).orElse(null), fotos.existe(usuario.getId()));
    }
}
