package com.vortise.gestion.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "invitaciones_equipo")
public class InvitacionEquipo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private Long titularId;
    @ElementCollection
    @CollectionTable(name = "invitacion_empresas", joinColumns = @JoinColumn(name = "invitacion_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"invitacion_id", "empresa_id"}))
    @Column(name = "empresa_id", nullable = false) private Set<Long> empresaIds = new HashSet<>();
    @Column(nullable = false) private Instant creadoEn;
    @Column(nullable = false) private Instant venceEn;
    private Instant revocadoEn;
    private Instant consumidoEn;
    protected InvitacionEquipo() { }
    public InvitacionEquipo(String hash, Long titularId, Set<Long> empresas, Instant now) {
        tokenHash = hash; this.titularId = titularId; empresaIds = new HashSet<>(empresas);
        creadoEn = now; venceEn = now.plus(java.time.Duration.ofHours(72));
    }
    public Long getId() { return id; }
    public Long getTitularId() { return titularId; }
    public Set<Long> getEmpresaIds() { return Set.copyOf(empresaIds); }
    public Instant getVenceEn() { return venceEn; }
    public boolean disponible(Instant now) { return revocadoEn == null && consumidoEn == null && now.isBefore(venceEn); }
    public String estado(Instant now) { return consumidoEn != null ? "ACEPTADA" : revocadoEn != null ? "REVOCADA" : !now.isBefore(venceEn) ? "VENCIDA" : "PENDIENTE"; }
    public void consumir(Instant now) { consumidoEn = now; }
    public void revocar(Instant now) { revocadoEn = now; }
}
