package com.vortise.gestion.service;

import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClerkUserSyncService {
    private static final Logger log = LoggerFactory.getLogger(ClerkUserSyncService.class);
    
    private final UsuarioEmpresaRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    
    @Value("${app.clerk.enabled:false}")
    private boolean clerkEnabled;

    @Transactional
    public UsuarioEmpresa sincronizarDesdeClerk(String clerkUserId, String email) {
        if (!clerkEnabled) {
            log.debug("Clerk disabled; sync skipped");
            return null;
        }

        // 1. Buscar por clerkUserId (usuario ya migrado)
        Optional<UsuarioEmpresa> existing = usuarioRepository.findByClerkUserId(clerkUserId);
        if (existing.isPresent()) {
            log.debug("Found existing user by clerkUserId: {}", clerkUserId);
            return existing.get();
        }

        // 2. Buscar por email (usuario pre-existente a migrar)
        email = email.trim().toLowerCase();
        Optional<UsuarioEmpresa> preExistente = usuarioRepository.findByEmail(email);
        if (preExistente.isPresent()) {
            UsuarioEmpresa usuario = preExistente.get();
            usuario.setClerkUserId(clerkUserId);
            usuario.setMigradoAClerk(true);
            usuarioRepository.save(usuario);
            log.info("Migrated pre-existing user {} to Clerk", email);
            return usuario;
        }

        // 3. No existe: crear nuevo usuario
        log.info("Creating new Clerk user for email: {}", email);
        UsuarioEmpresa nuevoUsuario = new UsuarioEmpresa();
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setClerkUserId(clerkUserId);
        nuevoUsuario.setNombre(email.split("@")[0]);
        nuevoUsuario.setRol(RolEmpresa.CONSULTA);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setMigradoAClerk(true);
        nuevoUsuario.setTieneLogin(true);
        
        // Asignar empresa default (PLATFORM)
        var empresaDefault = empresaRepository.findByCodigoEmpresa("PLATFORM-0001")
            .orElseThrow(() -> new RuntimeException("PLATFORM empresa no encontrada"));
        nuevoUsuario.setEmpresa(empresaDefault);
        
        return usuarioRepository.save(nuevoUsuario);
    }

    @Transactional
    public void prepararParaInvitacion(UsuarioEmpresa usuario) {
        usuario.setTieneLogin(false);
        usuario.setCodigoInvitacion(UUID.randomUUID().toString());
        usuario.setInvitacionExpiraEn(Instant.now().plus(java.time.Duration.ofDays(7)));
        usuarioRepository.save(usuario);
        log.info("User {} prepared for invitation with code {}", usuario.getEmail(), usuario.getCodigoInvitacion());
    }

    @Transactional
    public UsuarioEmpresa aceptarInvitacion(UsuarioEmpresa usuario, String clerkUserId) {
        if (!usuario.esInvitacionValida()) {
            throw new IllegalStateException("Invitation code has expired or is invalid");
        }
        usuario.setClerkUserId(clerkUserId);
        usuario.setTieneLogin(true);
        usuario.setCodigoInvitacion(null);
        usuario.setInvitacionExpiraEn(null);
        usuario.setMigradoAClerk(true);
        usuarioRepository.save(usuario);
        log.info("User {} accepted invitation", usuario.getEmail());
        return usuario;
    }
}
