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
    private Long intentos;
    private Instant proximoIntento;
    private String estadoFactura;
    private Instant fechaPago;
    private Instant periodoInicio;
    private Instant periodoFin;

    public void registrarDetalleFactura(Long intentos, Instant proximoIntento, String estadoFactura,
            Instant fechaPago, Instant periodoInicio, Instant periodoFin) {
        this.intentos = intentos; this.proximoIntento = proximoIntento; this.estadoFactura = estadoFactura;
        this.fechaPago = fechaPago; this.periodoInicio = periodoInicio; this.periodoFin = periodoFin;
    }
    public Long getIntentos() { return intentos; }
    public Instant getProximoIntento() { return proximoIntento; }
    public String getEstadoFactura() { return estadoFactura; }
    public Instant getFechaPago() { return fechaPago; }
    public Instant getPeriodoInicio() { return periodoInicio; }
    public Instant getPeriodoFin() { return periodoFin; }

    @Column(nullable = false) private long montoDevueltoCentavos;
    @jakarta.persistence.Version private long version;
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "devoluciones_pago", joinColumns = @JoinColumn(name = "pago_id"))
    @jakarta.persistence.MapKeyColumn(name = "stripe_refund_id")
    private java.util.Map<String, DevolucionPago> devoluciones = new java.util.HashMap<>();

    public void registrarDevolucion(String id, long monto, String estado, Instant fecha) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Devolución sin identificador");
        devoluciones.put(id, new DevolucionPago(monto, estado, fecha));
        montoDevueltoCentavos = devoluciones.values().stream().filter(d -> "succeeded".equals(d.getEstado()))
            .mapToLong(DevolucionPago::getMontoCentavos).sum();
    }
    public long getMontoDevueltoCentavos() {
        return montoDevueltoCentavos;
    }
    public long getMontoNetoCentavos() { return "PAGADO".equals(estado) ? montoCentavos - getMontoDevueltoCentavos() : 0; }
    public String getEstadoDevolucion() {
        if (devoluciones.isEmpty()) return "SIN_DEVOLUCION";
        long total = getMontoDevueltoCentavos();
        if (total > 0) return total >= montoCentavos ? "TOTAL" : "PARCIAL";
        return devoluciones.values().stream().anyMatch(d -> !java.util.Set.of("failed", "canceled", "succeeded").contains(d.getEstado()))
            ? "PENDIENTE" : "FALLIDA";
    }
    public Instant getFechaDevolucion() {
        return devoluciones.values().stream().map(DevolucionPago::getFecha).max(Instant::compareTo).orElse(null);
    }

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
