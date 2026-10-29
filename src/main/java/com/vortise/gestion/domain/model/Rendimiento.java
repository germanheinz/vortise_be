package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="rendimientos")
public class Rendimiento {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String fase;
    private String etapa;
    private String sistema;
    private String rol;
    @Column(name = "hh_porm2", nullable=false)
    private Double hhPorM2;
    @Column(nullable=false)
    private Double precioPorHh;

    public static RendimientoBuilder builder() {
        return new RendimientoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getFase() {
        return this.fase;
    }

    public String getEtapa() {
        return this.etapa;
    }

    public String getSistema() {
        return this.sistema;
    }

    public String getRol() {
        return this.rol;
    }

    public Double getHhPorM2() {
        return this.hhPorM2;
    }

    public Double getPrecioPorHh() {
        return this.precioPorHh;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFase(String fase) {
        this.fase = fase;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public void setHhPorM2(Double hhPorM2) {
        this.hhPorM2 = hhPorM2;
    }

    public void setPrecioPorHh(Double precioPorHh) {
        this.precioPorHh = precioPorHh;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        Rendimiento other = (Rendimiento) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof Rendimiento;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "Rendimiento(id=" + getId() + ")"; }

    public Rendimiento() {
    }

    public Rendimiento(Long id, String fase, String etapa, String sistema, String rol, Double hhPorM2, Double precioPorHh) {
        this.id = id;
        this.fase = fase;
        this.etapa = etapa;
        this.sistema = sistema;
        this.rol = rol;
        this.hhPorM2 = hhPorM2;
        this.precioPorHh = precioPorHh;
    }

    public static class RendimientoBuilder {
        private Long id;
        private String fase;
        private String etapa;
        private String sistema;
        private String rol;
        private Double hhPorM2;
        private Double precioPorHh;

        RendimientoBuilder() {
        }

        public RendimientoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RendimientoBuilder fase(String fase) {
            this.fase = fase;
            return this;
        }

        public RendimientoBuilder etapa(String etapa) {
            this.etapa = etapa;
            return this;
        }

        public RendimientoBuilder sistema(String sistema) {
            this.sistema = sistema;
            return this;
        }

        public RendimientoBuilder rol(String rol) {
            this.rol = rol;
            return this;
        }

        public RendimientoBuilder hhPorM2(Double hhPorM2) {
            this.hhPorM2 = hhPorM2;
            return this;
        }

        public RendimientoBuilder precioPorHh(Double precioPorHh) {
            this.precioPorHh = precioPorHh;
            return this;
        }

        public Rendimiento build() {
            return new Rendimiento(this.id, this.fase, this.etapa, this.sistema, this.rol, this.hhPorM2, this.precioPorHh);
        }

        public String toString() { return "RendimientoBuilder"; }
    }
}
