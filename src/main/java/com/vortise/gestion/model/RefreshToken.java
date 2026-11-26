package com.vortise.gestion.model;

import jakarta.persistence.*;
import java.time.Instant;

/** One link of a rotating refresh-token chain (a "family"). Only the SHA-256 of the token is stored. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(nullable = false, length = 36) private String familia;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private Instant creadoEn;
    @Column(nullable = false) private Instant inicioFamilia;
    @Column(nullable = false) private Instant venceEn;
    private Instant usadoEn;
    private Instant revocadoEn;
    @Column(nullable = false) private long sessionVersion;

    protected RefreshToken() { }

    public RefreshToken(Long usuarioId, String familia, String tokenHash, Instant now, Instant inicioFamilia, Instant venceEn, long sessionVersion) {
        this.usuarioId = usuarioId; this.familia = familia; this.tokenHash = tokenHash; this.creadoEn = now;
        this.inicioFamilia = inicioFamilia; this.venceEn = venceEn; this.sessionVersion = sessionVersion;
    }

    public void marcarUsado(Instant now) { usadoEn = now; }
    public void revocar(Instant now) { if (revocadoEn == null) revocadoEn = now; }
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getFamilia() { return familia; }
    public String getTokenHash() { return tokenHash; }
    public Instant getInicioFamilia() { return inicioFamilia; }
    public Instant getVenceEn() { return venceEn; }
    public Instant getUsadoEn() { return usadoEn; }
    public Instant getRevocadoEn() { return revocadoEn; }
    public long getSessionVersion() { return sessionVersion; }
}
