package com.vortise.gestion.service.usuario;

import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClerkUserSyncService {
    private static final Logger log = LoggerFactory.getLogger(ClerkUserSyncService.class);
    
    private final UsuarioEmpresaRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    
    @Value("${app.clerk.enabled:false}")
    private boolean clerkEnabled;

    public ClerkUserSyncService(UsuarioEmpresaRepository usuarioRepository, EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

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

        // 2. Buscar por email (usuario pre-existente o invitado)
        email = email.trim().toLowerCase();
        Optional<UsuarioEmpresa> preExistente = usuarioRepository.findByEmail(email);
        if (preExistente.isPresent()) {
            UsuarioEmpresa usuario = preExistente.get();
            
            // Si fue invitado, invalidar código de invitación
            if (usuario.getCodigoInvitacion() != null) {
                log.info("Accepting invitation for user: {}", email);
                usuario.setCodigoInvitacion(null);
                usuario.setInvitacionExpiraEn(null);
            }
            
            usuario.setClerkUserId(clerkUserId);
            usuario.setMigradoAClerk(true);
            usuario.setTieneLogin(true);
            usuarioRepository.save(usuario);
            log.info("Migrated user {} to Clerk", email);
            return usuario;
        }

        // 3. No existe: crear nuevo usuario
        log.info("Creating new Clerk user for email: {}", email);
        
        // Asignar empresa default (PLATFORM)
        var empresaDefault = empresaRepository.findByIdentificacionFiscal("PLATFORM-0001")
            .orElseThrow(() -> new RuntimeException("PLATFORM empresa no encontrada"));
        
        UsuarioEmpresa nuevoUsuario = new UsuarioEmpresa(empresaDefault, email, email.split("@")[0], "", RolEmpresa.CONSULTA);
        nuevoUsuario.setClerkUserId(clerkUserId);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setMigradoAClerk(true);
        nuevoUsuario.setTieneLogin(true);
        
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
