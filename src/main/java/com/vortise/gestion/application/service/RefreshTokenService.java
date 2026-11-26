package com.vortise.gestion.application.service;

import com.vortise.gestion.application.audit.AuditLog;
import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.domain.model.RefreshToken;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.RefreshTokenRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rotating refresh tokens. Each use returns a new token and retires the old one. Presenting a retired
 * token again (outside a short grace for concurrent tabs) revokes the whole family: it was stolen or replayed.
 */
@Service
public class RefreshTokenService {
    public static final long TTL_SEGUNDOS = 7L * 24 * 3600;
    static final long MAX_FAMILIA_SEGUNDOS = 30L * 24 * 3600;
    static final long GRACIA_SEGUNDOS = 10;
    private static final long RETENCION_SEGUNDOS = 24L * 3600;

    private final RefreshTokenRepository tokens;
    private final UsuarioEmpresaRepository usuarios;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, UsuarioEmpresaRepository usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    /** Starts a new family for a fresh login and returns the raw token (never stored). */
    @Transactional
    public String emitir(Long usuarioId) {
        UsuarioEmpresa usuario = usuarios.findById(usuarioId).orElseThrow(SesionInvalidaException::new);
        Instant now = Instant.now();
        tokens.limpiarVencidos(now.minusSeconds(RETENCION_SEGUNDOS));
        return crear(usuario.getId(), UUID.randomUUID().toString(), now, now, usuario.getSessionVersion());
    }

    public record Rotacion(Long usuarioId, String token) { }

    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Rotacion rotar(String raw) {
        if (raw == null || raw.isBlank()) throw new SesionInvalidaException();
        Instant now = Instant.now();
        RefreshToken actual = tokens.bloquearPorHash(PasswordResetTransactions.hash(raw)).orElseThrow(SesionInvalidaException::new);
        if (actual.getRevocadoEn() != null) throw new SesionInvalidaException();
        if (actual.getUsadoEn() != null && actual.getUsadoEn().plusSeconds(GRACIA_SEGUNDOS).isBefore(now)) {
            tokens.revocarFamilia(actual.getFamilia(), now);
            AuditLog.refreshReuseDetected(actual.getUsuarioId());
            throw new SesionInvalidaException();
        }
        if (!actual.getVenceEn().isAfter(now)) throw new SesionInvalidaException();
        UsuarioEmpresa usuario = usuarios.findById(actual.getUsuarioId()).filter(UsuarioEmpresa::isActivo).orElse(null);
        // A password, role, site or status change bumps the session version and ends every refresh chain.
        if (usuario == null || usuario.getSessionVersion() != actual.getSessionVersion()) {
            tokens.revocarFamilia(actual.getFamilia(), now);
            throw new SesionInvalidaException();
        }
        Instant tope = actual.getInicioFamilia().plusSeconds(MAX_FAMILIA_SEGUNDOS);
        if (!tope.isAfter(now)) {
            tokens.revocarFamilia(actual.getFamilia(), now);
            throw new SesionInvalidaException();
        }
        if (actual.getUsadoEn() == null) actual.marcarUsado(now);
        tokens.save(actual);
        return new Rotacion(usuario.getId(), crear(usuario.getId(), actual.getFamilia(), now, actual.getInicioFamilia(), usuario.getSessionVersion()));
    }

    /** Logout: ends the chain of the presented token. Unknown or empty tokens are ignored. */
    @Transactional
    public void cerrar(String raw) {
        if (raw == null || raw.isBlank()) return;
        tokens.bloquearPorHash(PasswordResetTransactions.hash(raw))
            .ifPresent(token -> tokens.revocarFamilia(token.getFamilia(), Instant.now()));
    }

    @Transactional
    public void revocarUsuario(Long usuarioId) {
        tokens.revocarPorUsuario(usuarioId, Instant.now());
    }

    private String crear(Long usuarioId, String familia, Instant now, Instant inicioFamilia, long sessionVersion) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant vence = now.plusSeconds(TTL_SEGUNDOS);
        Instant tope = inicioFamilia.plusSeconds(MAX_FAMILIA_SEGUNDOS);
        tokens.save(new RefreshToken(usuarioId, familia, PasswordResetTransactions.hash(raw), now, inicioFamilia,
            vence.isAfter(tope) ? tope : vence, sessionVersion));
        return raw;
    }
}
