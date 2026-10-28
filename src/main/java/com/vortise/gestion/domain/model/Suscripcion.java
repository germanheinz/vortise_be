package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;

/** Current Stripe subscription of one holder (a user or a whole company). */
@Entity
@Table(name = "suscripciones")
public class Suscripcion {
    private static final Set<String> ESTADOS_ACTIVOS = Set.of("trialing", "active");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TitularSuscripcion titular;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", unique = true) private UsuarioEmpresa usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "empresa_id", unique = true) private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "plan_id") private PlanSuscripcion plan;
    @Column(nullable = false, unique = true) private String stripeCustomerId;
    @Column(unique = true) private String stripeSubscriptionId;
    private String estado;
    private Instant inicio;
    private Instant finPrueba;
    private Instant finPeriodoActual;
    @Column(nullable = false) private boolean cancelaAlFinal;
    @Column(nullable = false) private boolean pruebaUsada;
    @Column(nullable = false) private Instant actualizadoEn = Instant.now();

    protected Suscripcion() { }

    public static Suscripcion paraUsuario(UsuarioEmpresa usuario, String stripeCustomerId) {
        Suscripcion s = new Suscripcion(); s.titular = TitularSuscripcion.USUARIO; s.usuario = usuario; s.stripeCustomerId = stripeCustomerId; return s;
    }

    public static Suscripcion paraEmpresa(Empresa empresa, String stripeCustomerId) {
        Suscripcion s = new Suscripcion(); s.titular = TitularSuscripcion.EMPRESA; s.empresa = empresa; s.stripeCustomerId = stripeCustomerId; return s;
    }

    public boolean isActiva(Instant now) {
        return estado != null && ESTADOS_ACTIVOS.contains(estado) && (finPeriodoActual == null || finPeriodoActual.isAfter(now));
    }

    public void actualizarDesdeStripe(String subscriptionId, PlanSuscripcion plan, String estado, Instant finPrueba,
            Instant finPeriodoActual, boolean cancelaAlFinal) {
        this.stripeSubscriptionId = subscriptionId;
        if (plan != null) this.plan = plan;
        this.estado = estado;
        this.finPrueba = finPrueba;
        this.finPeriodoActual = finPeriodoActual;
        this.cancelaAlFinal = cancelaAlFinal;
        if (finPrueba != null) this.pruebaUsada = true;
        this.actualizadoEn = Instant.now();
    }

    public void registrarInicio(Instant inicio) { this.inicio = inicio; }
    public Instant getInicio() { return inicio; }

    public Long getId() { return id; }
    public TitularSuscripcion getTitular() { return titular; }
    public UsuarioEmpresa getUsuario() { return usuario; }
    public Empresa getEmpresa() { return empresa; }
    public PlanSuscripcion getPlan() { return plan; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getEstado() { return estado; }
    public Instant getFinPrueba() { return finPrueba; }
    public Instant getFinPeriodoActual() { return finPeriodoActual; }
    public boolean isCancelaAlFinal() { return cancelaAlFinal; }
    public boolean isPruebaUsada() { return pruebaUsada; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
