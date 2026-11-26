package com.vortise.gestion.service.usuario;

import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClerkUserSyncService {
    private static final Logger log = LoggerFactory.getLogger(ClerkUserSyncService.class);
    
    private final UsuarioEmpresaRepository usuarioRepository;
    
    @Value("${app.clerk.enabled:false}")
    private boolean clerkEnabled;

    public ClerkUserSyncService(UsuarioEmpresaRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<UsuarioEmpresa> buscarPorClerkUserId(String clerkUserId) {
        return clerkEnabled ? usuarioRepository.findByClerkUserId(clerkUserId) : Optional.empty();
    }

    /**
     * Links a Clerk account to an existing user. The email must be one Clerk reports as verified for that account.
     * Unknown emails get no account: access always comes from an invitation or an administrator.
     */
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
        if (email == null || email.isBlank()) return null;

        // 2. Buscar por email (usuario pre-existente o invitado)
        email = email.trim().toLowerCase(Locale.ROOT);
        Optional<UsuarioEmpresa> preExistente = usuarioRepository.findByEmail(email);
        if (preExistente.isEmpty()) {
            log.debug("No local user for Clerk account {}", clerkUserId);
            return null;
        }
        UsuarioEmpresa usuario = preExistente.get();
        if (usuario.getClerkUserId() != null && !usuario.getClerkUserId().equals(clerkUserId)) {
            log.warn("User {} is already linked to another Clerk account", usuario.getId());
            return null;
        }

        // Si fue invitado, invalidar código de invitación
        if (usuario.getCodigoInvitacion() != null) {
            log.info("Accepting invitation for user: {}", usuario.getId());
            usuario.setCodigoInvitacion(null);
            usuario.setInvitacionExpiraEn(null);
        }

        usuario.setClerkUserId(clerkUserId);
        usuario.setMigradoAClerk(true);
        usuario.setTieneLogin(true);
        usuarioRepository.save(usuario);
        log.info("Linked user {} to Clerk", usuario.getId());
        return usuario;
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
