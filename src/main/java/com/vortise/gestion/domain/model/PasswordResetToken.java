package com.vortise.gestion.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

/** One current token per account. Never stores the bearer token itself. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id private Long usuarioId;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private Instant creadoEn;
    @Column(nullable = false) private Instant venceEn;
    private Instant consumidoEn;
    @Column(nullable = false) private long sessionVersion;
    protected PasswordResetToken() { }
    public PasswordResetToken(Long usuarioId, String hash, Instant now, long version) { this.usuarioId = usuarioId; renovar(hash, now, version); }
    public void renovar(String hash, Instant now, long version) {
        tokenHash = hash; creadoEn = now; venceEn = now.plusSeconds(1800); consumidoEn = null; sessionVersion = version;
    }
    public boolean valido(String hash, Instant now, long version) {
        return consumidoEn == null && venceEn.isAfter(now) && tokenHash.equals(hash) && sessionVersion == version;
    }
    public void consumir(Instant now) { consumidoEn = now; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getVenceEn() { return venceEn; }
}
