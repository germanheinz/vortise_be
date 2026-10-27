package com.vortise.gestion.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.time.Instant;

@Embeddable
public class DevolucionPago {
    @Column(nullable = false) private long montoCentavos;
    @Column(nullable = false) private String estado;
    @Column(nullable = false) private Instant fecha;
    protected DevolucionPago() { }
    public DevolucionPago(long monto, String estado, Instant fecha) {
        if (monto < 0 || estado == null || fecha == null) throw new IllegalArgumentException("Devolución inválida");
        this.montoCentavos = monto; this.estado = estado; this.fecha = fecha;
    }
    public long getMontoCentavos() { return montoCentavos; }
    public String getEstado() { return estado; }
    public Instant getFecha() { return fecha; }
}
