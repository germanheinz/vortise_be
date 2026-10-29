package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="gastos_fijos")
public class GastoFijo {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String concepto;
    @Column(nullable=false)
    private Double monto;
    @Column
    private String categoria;
    private Integer mes;

    public static GastoFijoBuilder builder() {
        return new GastoFijoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getConcepto() {
        return this.concepto;
    }

    public Double getMonto() {
        return this.monto;
    }

    public String getCategoria() {
        return this.categoria;
    }

    public Integer getMes() {
        return this.mes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        GastoFijo other = (GastoFijo) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof GastoFijo;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "GastoFijo(id=" + getId() + ")"; }

    public GastoFijo() {
    }

    public GastoFijo(Long id, String concepto, Double monto, String categoria, Integer mes) {
        this.id = id;
        this.concepto = concepto;
        this.monto = monto;
        this.categoria = categoria;
        this.mes = mes;
    }

    public static class GastoFijoBuilder {
        private Long id;
        private String concepto;
        private Double monto;
        private String categoria;
        private Integer mes;

        GastoFijoBuilder() {
        }

        public GastoFijoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GastoFijoBuilder concepto(String concepto) {
            this.concepto = concepto;
            return this;
        }

        public GastoFijoBuilder monto(Double monto) {
            this.monto = monto;
            return this;
        }

        public GastoFijoBuilder categoria(String categoria) {
            this.categoria = categoria;
            return this;
        }

        public GastoFijoBuilder mes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public GastoFijo build() {
            return new GastoFijo(this.id, this.concepto, this.monto, this.categoria, this.mes);
        }

        public String toString() { return "GastoFijoBuilder"; }
    }
}
