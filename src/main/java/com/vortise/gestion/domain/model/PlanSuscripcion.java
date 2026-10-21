package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "planes_suscripcion")
public class PlanSuscripcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String codigo;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private long precioMensualCentavos;
    @Column(nullable = false) private String moneda;
    @Column(nullable = false) private int diasPrueba;
    /** Higher level wins when a user is covered by several subscriptions. */
    @Column(nullable = false) private int nivel;
    @Column(nullable = false, length = 2000) private String beneficios;
    private String stripePriceId;
    @Column(nullable = false) private boolean activo = true;

    protected PlanSuscripcion() { }

    public PlanSuscripcion(String codigo, String nombre, long precioMensualCentavos, String moneda, int diasPrueba, int nivel, List<String> beneficios) {
        this.codigo = codigo; this.nombre = nombre; this.precioMensualCentavos = precioMensualCentavos; this.moneda = moneda;
        this.diasPrueba = diasPrueba; this.nivel = nivel; this.beneficios = String.join("\n", beneficios);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public long getPrecioMensualCentavos() { return precioMensualCentavos; }
    public String getMoneda() { return moneda; }
    public int getDiasPrueba() { return diasPrueba; }
    public int getNivel() { return nivel; }
    public List<String> getBeneficios() { return beneficios.isBlank() ? List.of() : Arrays.asList(beneficios.split("\n")); }
    public String getStripePriceId() { return stripePriceId; }
    public boolean isActivo() { return activo; }
    public void setStripePriceId(String value) { stripePriceId = value; }
}
