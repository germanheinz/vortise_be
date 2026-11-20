package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "altas_restriccion_chat")
public class AltaRestriccionChat {
    @Id
    @Column(name = "clave_hash", length = 64)
    private String claveHash;

    @Column(name = "solicitud_hash", nullable = false, length = 64)
    private String solicitudHash;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "restriccion_id", nullable = false)
    private Long restriccionId;

    protected AltaRestriccionChat() { }

    public AltaRestriccionChat(String claveHash, String solicitudHash, Long proyectoId, Long usuarioId, Long restriccionId) {
        this.claveHash = claveHash;
        this.solicitudHash = solicitudHash;
        this.proyectoId = proyectoId;
        this.usuarioId = usuarioId;
        this.restriccionId = restriccionId;
    }

    public boolean coincideCon(String hash) {
        return solicitudHash.equals(hash);
    }

    public Long getRestriccionId() {
        return restriccionId;
    }
}
