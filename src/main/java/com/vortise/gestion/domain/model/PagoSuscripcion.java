package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pagos_suscripcion")
public class PagoSuscripcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;
    @Column(nullable = false, unique = true) private String stripeInvoiceId;
    @Column(nullable = false) private long montoCentavos;
    @Column(nullable = false) private String moneda;
    @Column(nullable = false) private String estado;
    @Column(nullable = false) private Instant fecha;

    protected PagoSuscripcion() { }

    public PagoSuscripcion(Suscripcion suscripcion, String stripeInvoiceId) {
        this.suscripcion = suscripcion; this.stripeInvoiceId = stripeInvoiceId;
    }

    public void registrar(long montoCentavos, String moneda, String estado, Instant fecha) {
        this.montoCentavos = montoCentavos; this.moneda = moneda; this.estado = estado; this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Suscripcion getSuscripcion() { return suscripcion; }
    public String getStripeInvoiceId() { return stripeInvoiceId; }
    public long getMontoCentavos() { return montoCentavos; }
    public String getMoneda() { return moneda; }
    public String getEstado() { return estado; }
    public Instant getFecha() { return fecha; }
}
