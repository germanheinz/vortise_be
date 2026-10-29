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
@Table(name="costos_indirectos")
public class CostoIndirecto {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="proyecto_id", nullable=false)
    private Proyecto proyecto;
    @Column(nullable=false)
    private String concepto;
    @Column(nullable=false)
    private Double monto;
    private Integer mes;

    public static CostoIndirectoBuilder builder() {
        return new CostoIndirectoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public Proyecto getProyecto() {
        return this.proyecto;
    }

    public String getConcepto() {
        return this.concepto;
    }

    public Double getMonto() {
        return this.monto;
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

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        CostoIndirecto other = (CostoIndirecto) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof CostoIndirecto;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "CostoIndirecto(id=" + getId() + ")"; }

    public CostoIndirecto() {
    }

    public CostoIndirecto(Long id, Proyecto proyecto, String concepto, Double monto, Integer mes) {
        this.id = id;
        this.proyecto = proyecto;
        this.concepto = concepto;
        this.monto = monto;
        this.mes = mes;
    }

    public static class CostoIndirectoBuilder {
        private Long id;
        private Proyecto proyecto;
        private String concepto;
        private Double monto;
        private Integer mes;

        CostoIndirectoBuilder() {
        }

        public CostoIndirectoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CostoIndirectoBuilder proyecto(Proyecto proyecto) {
            this.proyecto = proyecto;
            return this;
        }

        public CostoIndirectoBuilder concepto(String concepto) {
            this.concepto = concepto;
            return this;
        }

        public CostoIndirectoBuilder monto(Double monto) {
            this.monto = monto;
            return this;
        }

        public CostoIndirectoBuilder mes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public CostoIndirecto build() {
            return new CostoIndirecto(this.id, this.proyecto, this.concepto, this.monto, this.mes);
        }

        public String toString() { return "CostoIndirectoBuilder"; }
    }
}
