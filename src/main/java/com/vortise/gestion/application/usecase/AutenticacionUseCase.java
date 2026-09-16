package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionUseCase {
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacionUseCase(UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDto login(LoginRequestDto request) {
        UsuarioEmpresa usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
            .filter(item -> item.isActivo())
            .filter(item -> passwordEncoder.matches(request.password(), item.getPasswordHash()))
            .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));
        return new LoginResponseDto(jwtService.generate(usuario.getId(), usuario.getEmpresa().getId(), usuario.getEmail(), usuario.getRol().name()),
            usuario.getId(), usuario.getEmpresa().getId(), usuario.getEmpresa().getNombre(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }
}
