package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stripe_eventos_procesados")
public class StripeEventoProcesado {
    @Id private String eventId;
    @Column(nullable = false) private String tipo;
    @Column(nullable = false) private Instant procesadoEn = Instant.now();

    protected StripeEventoProcesado() { }

    public StripeEventoProcesado(String eventId, String tipo) { this.eventId = eventId; this.tipo = tipo; }

    public String getEventId() { return eventId; }
    public String getTipo() { return tipo; }
    public Instant getProcesadoEn() { return procesadoEn; }
}
