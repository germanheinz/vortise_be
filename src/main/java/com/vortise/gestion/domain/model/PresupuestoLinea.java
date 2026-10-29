package com.vortise.gestion.domain.model;

import com.vortise.gestion.domain.model.Proyecto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="presupuesto_lineas")
public class PresupuestoLinea {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="proyecto_id", nullable=false)
    private Proyecto proyecto;
    @Column(nullable=false)
    private String etapa;
    @Column(nullable=false)
    private String tipoSuperficie;
    @Column(nullable=false)
    private String funcion;
    private String sistema;
    private Double m2;
    @jakarta.persistence.Column(name = "hh_porm2")
    private Double hhPorM2;
    private Double precioPorHh;
    private Double precioPorHhUsd;
    private Integer mes;

    public static PresupuestoLineaBuilder builder() {
        return new PresupuestoLineaBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public Proyecto getProyecto() {
        return this.proyecto;
    }

    public String getEtapa() {
        return this.etapa;
    }

    public String getTipoSuperficie() {
        return this.tipoSuperficie;
    }

    public String getFuncion() {
        return this.funcion;
    }

    public String getSistema() {
        return this.sistema;
    }

    public Double getM2() {
        return this.m2;
    }

    public Double getHhPorM2() {
        return this.hhPorM2;
    }

    public Double getPrecioPorHh() {
        return this.precioPorHh;
    }

    public Double getPrecioPorHhUsd() {
        return this.precioPorHhUsd;
    }

    public Integer getMes() {
        return this.mes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public void setTipoSuperficie(String tipoSuperficie) {
        this.tipoSuperficie = tipoSuperficie;
    }

    public void setFuncion(String funcion) {
        this.funcion = funcion;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public void setM2(Double m2) {
        this.m2 = m2;
    }

    public void setHhPorM2(Double hhPorM2) {
        this.hhPorM2 = hhPorM2;
    }

    public void setPrecioPorHh(Double precioPorHh) {
        this.precioPorHh = precioPorHh;
    }

    public void setPrecioPorHhUsd(Double precioPorHhUsd) {
        this.precioPorHhUsd = precioPorHhUsd;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        PresupuestoLinea other = (PresupuestoLinea) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof PresupuestoLinea;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "PresupuestoLinea(id=" + getId() + ")"; }

    public PresupuestoLinea() {
    }

    public PresupuestoLinea(Long id, Proyecto proyecto, String etapa, String tipoSuperficie, String funcion, String sistema, Double m2, Double hhPorM2, Double precioPorHh, Double precioPorHhUsd, Integer mes) {
        this.id = id;
        this.proyecto = proyecto;
        this.etapa = etapa;
        this.tipoSuperficie = tipoSuperficie;
        this.funcion = funcion;
        this.sistema = sistema;
        this.m2 = m2;
        this.hhPorM2 = hhPorM2;
        this.precioPorHh = precioPorHh;
        this.precioPorHhUsd = precioPorHhUsd;
        this.mes = mes;
    }

    public static class PresupuestoLineaBuilder {
        private Long id;
        private Proyecto proyecto;
        private String etapa;
        private String tipoSuperficie;
        private String funcion;
        private String sistema;
        private Double m2;
        private Double hhPorM2;
        private Double precioPorHh;
        private Double precioPorHhUsd;
        private Integer mes;

        PresupuestoLineaBuilder() {
        }

        public PresupuestoLineaBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PresupuestoLineaBuilder proyecto(Proyecto proyecto) {
            this.proyecto = proyecto;
            return this;
        }

        public PresupuestoLineaBuilder etapa(String etapa) {
            this.etapa = etapa;
            return this;
        }

        public PresupuestoLineaBuilder tipoSuperficie(String tipoSuperficie) {
            this.tipoSuperficie = tipoSuperficie;
            return this;
        }

        public PresupuestoLineaBuilder funcion(String funcion) {
            this.funcion = funcion;
            return this;
        }

        public PresupuestoLineaBuilder sistema(String sistema) {
            this.sistema = sistema;
            return this;
        }

        public PresupuestoLineaBuilder m2(Double m2) {
            this.m2 = m2;
            return this;
        }

        public PresupuestoLineaBuilder hhPorM2(Double hhPorM2) {
            this.hhPorM2 = hhPorM2;
            return this;
        }

        public PresupuestoLineaBuilder precioPorHh(Double precioPorHh) {
            this.precioPorHh = precioPorHh;
            return this;
        }

        public PresupuestoLineaBuilder precioPorHhUsd(Double precioPorHhUsd) {
            this.precioPorHhUsd = precioPorHhUsd;
            return this;
        }

        public PresupuestoLineaBuilder mes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public PresupuestoLinea build() {
            return new PresupuestoLinea(this.id, this.proyecto, this.etapa, this.tipoSuperficie, this.funcion, this.sistema, this.m2, this.hhPorM2, this.precioPorHh, this.precioPorHhUsd, this.mes);
        }

        public String toString() { return "PresupuestoLineaBuilder"; }
    }
}
