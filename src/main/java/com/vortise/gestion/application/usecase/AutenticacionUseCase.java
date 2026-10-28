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

    public AutenticacionUseCase(UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            SuscripcionConsultaService suscripciones, FotoPerfilStorage fotos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.suscripciones = suscripciones;
        this.fotos = fotos;
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
        Optional<Suscripcion> plan = suscripciones.planEfectivo(usuario);
        return new LoginResponseDto(jwtService.generate(usuario.getId(), usuario.getEmpresa().getId(), usuario.getEmail(), usuario.getRol().name(), usuario.isSuperAdmin(), usuario.getSessionVersion()),
            usuario.getId(), usuario.getEmpresa().getId(), usuario.getEmpresa().getNombre(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(),
            usuario.isSuperAdmin(), plan.isPresent(), plan.map(s -> s.getPlan().getCodigo()).orElse(null), fotos.existe(usuario.getId()));
    }
}
