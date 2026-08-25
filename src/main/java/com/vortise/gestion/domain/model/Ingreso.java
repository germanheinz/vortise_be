/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 */
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
@Table(name="ingresos")
public class Ingreso {
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

    public static IngresoBuilder builder() {
        return new IngresoBuilder();
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
        if (o == this) {
            return true;
        }
        if (!(o instanceof Ingreso)) {
            return false;
        }
        Ingreso other = (Ingreso)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Double this$monto = this.getMonto();
        Double other$monto = other.getMonto();
        if (this$monto == null ? other$monto != null : !((Object)this$monto).equals(other$monto)) {
            return false;
        }
        Integer this$mes = this.getMes();
        Integer other$mes = other.getMes();
        if (this$mes == null ? other$mes != null : !((Object)this$mes).equals(other$mes)) {
            return false;
        }
        Proyecto this$proyecto = this.getProyecto();
        Proyecto other$proyecto = other.getProyecto();
        if (this$proyecto == null ? other$proyecto != null : !((Object)this$proyecto).equals(other$proyecto)) {
            return false;
        }
        String this$concepto = this.getConcepto();
        String other$concepto = other.getConcepto();
        return !(this$concepto == null ? other$concepto != null : !this$concepto.equals(other$concepto));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Ingreso;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Double $monto = this.getMonto();
        result = result * 59 + ($monto == null ? 43 : ((Object)$monto).hashCode());
        Integer $mes = this.getMes();
        result = result * 59 + ($mes == null ? 43 : ((Object)$mes).hashCode());
        Proyecto $proyecto = this.getProyecto();
        result = result * 59 + ($proyecto == null ? 43 : ((Object)$proyecto).hashCode());
        String $concepto = this.getConcepto();
        result = result * 59 + ($concepto == null ? 43 : $concepto.hashCode());
        return result;
    }

    public String toString() {
        return "Ingreso(id=" + this.getId() + ", proyecto=" + String.valueOf(this.getProyecto()) + ", concepto=" + this.getConcepto() + ", monto=" + this.getMonto() + ", mes=" + this.getMes() + ")";
    }

    public Ingreso() {
    }

    public Ingreso(Long id, Proyecto proyecto, String concepto, Double monto, Integer mes) {
        this.id = id;
        this.proyecto = proyecto;
        this.concepto = concepto;
        this.monto = monto;
        this.mes = mes;
    }

    public static class IngresoBuilder {
        private Long id;
        private Proyecto proyecto;
        private String concepto;
        private Double monto;
        private Integer mes;

        IngresoBuilder() {
        }

        public IngresoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IngresoBuilder proyecto(Proyecto proyecto) {
            this.proyecto = proyecto;
            return this;
        }

        public IngresoBuilder concepto(String concepto) {
            this.concepto = concepto;
            return this;
        }

        public IngresoBuilder monto(Double monto) {
            this.monto = monto;
            return this;
        }

        public IngresoBuilder mes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public Ingreso build() {
            return new Ingreso(this.id, this.proyecto, this.concepto, this.monto, this.mes);
        }

        public String toString() {
            return "Ingreso.IngresoBuilder(id=" + this.id + ", proyecto=" + String.valueOf(this.proyecto) + ", concepto=" + this.concepto + ", monto=" + this.monto + ", mes=" + this.mes + ")";
        }
    }
}
