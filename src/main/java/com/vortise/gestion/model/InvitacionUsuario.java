package com.vortise.gestion.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** One-time invitation for a person to join a company with a fixed role (and sites, for a jefe de obra). */
@Entity
@Table(name = "invitaciones_usuario")
public class InvitacionUsuario {
    public static final Duration VIGENCIA = Duration.ofHours(72);

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "creado_por_usuario_id", nullable = false) private Long creadoPorUsuarioId;
    @Column(nullable = false, length = 254) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RolEmpresa rol;
    @ElementCollection
    @CollectionTable(name = "invitacion_usuario_obras", joinColumns = @JoinColumn(name = "invitacion_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"invitacion_id", "proyecto_id"}))
    @Column(name = "proyecto_id", nullable = false) private Set<Long> obraIds = new HashSet<>();
    @Column(nullable = false) private Instant creadoEn;
    @Column(nullable = false) private Instant venceEn;
    private Instant revocadoEn;
    private Instant consumidoEn;

    protected InvitacionUsuario() { }

    public InvitacionUsuario(String tokenHash, Long empresaId, Long creadoPorUsuarioId, String email, RolEmpresa rol, Set<Long> obras, Instant now) {
        this.tokenHash = tokenHash; this.empresaId = empresaId; this.creadoPorUsuarioId = creadoPorUsuarioId;
        this.email = email; this.rol = rol; this.obraIds = new HashSet<>(obras);
        this.creadoEn = now; this.venceEn = now.plus(VIGENCIA);
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getEmail() { return email; }
    public RolEmpresa getRol() { return rol; }
    public Set<Long> getObraIds() { return Set.copyOf(obraIds); }
    public Instant getVenceEn() { return venceEn; }
    public boolean disponible(Instant now) { return revocadoEn == null && consumidoEn == null && now.isBefore(venceEn); }
    public String estado(Instant now) {
        return consumidoEn != null ? "ACEPTADA" : revocadoEn != null ? "REVOCADA" : !now.isBefore(venceEn) ? "VENCIDA" : "PENDIENTE";
    }
    public void consumir(Instant now) { consumidoEn = now; }
    public void revocar(Instant now) { revocadoEn = now; }
}
