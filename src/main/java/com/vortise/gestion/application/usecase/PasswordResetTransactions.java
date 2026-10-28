package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.domain.repository.PasswordResetTokenRepository;
import com.vortise.gestion.domain.repository.PasswordResetQuotaRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetTransactions {
    private final UsuarioEmpresaRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetQuotaRepository quotas;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    public PasswordResetTransactions(UsuarioEmpresaRepository usuarios, PasswordResetTokenRepository tokens,
            PasswordResetQuotaRepository quotas, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.tokens = tokens; this.quotas = quotas; this.encoder = encoder;
    }
    public record Delivery(String email, String token) {
        @Override public String toString() { return "PasswordResetDelivery[redacted]"; }
    }
    @Transactional
    public Optional<Delivery> issue(String email, String ip) {
        Instant now = Instant.now();
        // A seeded singleton serializes quota reservations across instances, including unknown emails.
        var global = quotas.bloquearGuard().orElseThrow(() -> new IllegalStateException("Password reset quota guard missing"));
        quotas.limpiar(now.minusSeconds(7200));
        var address = quotas.findById("ip:" + hash(ip)).orElseGet(() -> new PasswordResetQuota("ip:" + hash(ip), now));
        var account = quotas.findById("email:" + hash(email)).orElseGet(() -> new PasswordResetQuota("email:" + hash(email), now));
        if (!global.disponible(now, 60, 60) || !address.disponible(now, 900, 5) || !account.disponible(now, 60, 1)) return Optional.empty();
        global.registrar(); address.registrar(); account.registrar();
        quotas.save(global); quotas.save(address); quotas.save(account);
        var usuario = usuarios.bloquearPorEmail(email).filter(UsuarioEmpresa::isActivo);
        if (usuario.isEmpty()) return Optional.empty();
        var user = usuario.get(); byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var token = tokens.findById(user.getId()).orElseGet(() -> new PasswordResetToken(user.getId(), hash(raw), now, user.getSessionVersion()));
        token.renovar(hash(raw), now, user.getSessionVersion()); tokens.save(token);
        return Optional.of(new Delivery(user.getEmail(), raw));
    }
    @Transactional
    public String confirm(String raw, String password) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        if (password == null || password.length() < 8 || password.length() > 72 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new SolicitudInvalidaException("La contraseña debe tener entre 8 y 72 caracteres y no superar 72 bytes.");
        }
        String hash = hash(raw);
        var userId = tokens.buscarUsuarioPorHash(hash).orElseThrow(PasswordResetTransactions::invalid);
        var user = usuarios.bloquearPorId(userId).filter(UsuarioEmpresa::isActivo).orElseThrow(PasswordResetTransactions::invalid);
        // Re-read AFTER locking the account; a competing reset may already have consumed/replaced it.
        var token = tokens.findById(user.getId()).orElseThrow(PasswordResetTransactions::invalid);
        Instant now = Instant.now();
        if (!token.valido(hash, now, user.getSessionVersion())) throw invalid();
        user.setPasswordHash(encoder.encode(password)); token.consumir(now);
        usuarios.save(user); tokens.save(token);
        return user.getEmail();
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    private static SolicitudInvalidaException invalid() { return new SolicitudInvalidaException("El enlace no es válido o venció. Solicitá uno nuevo."); }
}
