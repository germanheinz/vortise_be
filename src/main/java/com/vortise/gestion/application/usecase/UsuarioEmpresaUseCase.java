package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.UsuarioEmpresaDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioEmpresaUseCase {
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioEmpresaUseCase(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository; this.usuarioRepository = usuarioRepository; this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioEmpresaDto> getAll(Long empresaId) { requireEmpresa(empresaId); return usuarioRepository.findByEmpresaId(empresaId).stream().map(this::toDto).toList(); }
    public UsuarioEmpresaDto create(Long empresaId, UsuarioEmpresaDto dto) {
        Empresa empresa = requireEmpresa(empresaId);
        if (dto.password() == null || dto.password().length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        usuarioRepository.findByEmail(dto.email().trim().toLowerCase()).ifPresent(existing -> { throw new IllegalArgumentException("Ya existe un usuario con ese email"); });
        long activos = usuarioRepository.countByEmpresaIdAndActivoTrue(empresaId);
        if (activos >= empresa.getLimiteUsuarios()) {
            throw new IllegalStateException("Se alcanzó el límite de " + empresa.getLimiteUsuarios() + " usuarios para esta empresa");
        }
        return toDto(usuarioRepository.save(new UsuarioEmpresa(empresa, dto.email().trim().toLowerCase(), dto.nombre().trim(), passwordEncoder.encode(dto.password()), dto.rol())));
    }
    public UsuarioEmpresaDto update(Long empresaId, Long id, UsuarioEmpresaDto dto) {
        Empresa empresa = requireEmpresa(empresaId);
        UsuarioEmpresa usuario = findOwned(empresaId, id);
        if (dto.activo() && !usuario.isActivo() && usuarioRepository.countByEmpresaIdAndActivoTrue(empresaId) >= empresa.getLimiteUsuarios()) {
            throw new IllegalStateException("Se alcanzó el límite de " + empresa.getLimiteUsuarios() + " usuarios para esta empresa");
        }
        usuario.setNombre(dto.nombre().trim()); usuario.setRol(dto.rol()); usuario.setActivo(dto.activo());
        if (dto.password() != null && !dto.password().isBlank()) usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        return toDto(usuarioRepository.save(usuario));
    }
    public void delete(Long empresaId, Long id) { requireEmpresa(empresaId); UsuarioEmpresa usuario = findOwned(empresaId, id); usuario.setActivo(false); usuarioRepository.save(usuario); }
    private Empresa requireEmpresa(Long id) { return empresaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada")); }
    private UsuarioEmpresa findOwned(Long empresaId, Long id) { UsuarioEmpresa usuario = usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado")); if (!usuario.getEmpresa().getId().equals(empresaId)) throw new IllegalArgumentException("El usuario no pertenece a la empresa"); return usuario; }
    private UsuarioEmpresaDto toDto(UsuarioEmpresa u) { return new UsuarioEmpresaDto(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.isActivo(), null); }
}
