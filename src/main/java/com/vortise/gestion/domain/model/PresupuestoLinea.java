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
        if (o == this) {
            return true;
        }
        if (!(o instanceof PresupuestoLinea)) {
            return false;
        }
        PresupuestoLinea other = (PresupuestoLinea)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Double this$m2 = this.getM2();
        Double other$m2 = other.getM2();
        if (this$m2 == null ? other$m2 != null : !((Object)this$m2).equals(other$m2)) {
            return false;
        }
        Double this$hhPorM2 = this.getHhPorM2();
        Double other$hhPorM2 = other.getHhPorM2();
        if (this$hhPorM2 == null ? other$hhPorM2 != null : !((Object)this$hhPorM2).equals(other$hhPorM2)) {
            return false;
        }
        Double this$precioPorHh = this.getPrecioPorHh();
        Double other$precioPorHh = other.getPrecioPorHh();
        if (this$precioPorHh == null ? other$precioPorHh != null : !((Object)this$precioPorHh).equals(other$precioPorHh)) {
            return false;
        }
        Double this$precioPorHhUsd = this.getPrecioPorHhUsd();
        Double other$precioPorHhUsd = other.getPrecioPorHhUsd();
        if (this$precioPorHhUsd == null ? other$precioPorHhUsd != null : !((Object)this$precioPorHhUsd).equals(other$precioPorHhUsd)) {
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
        String this$etapa = this.getEtapa();
        String other$etapa = other.getEtapa();
        if (this$etapa == null ? other$etapa != null : !this$etapa.equals(other$etapa)) {
            return false;
        }
        String this$tipoSuperficie = this.getTipoSuperficie();
        String other$tipoSuperficie = other.getTipoSuperficie();
        if (this$tipoSuperficie == null ? other$tipoSuperficie != null : !this$tipoSuperficie.equals(other$tipoSuperficie)) {
            return false;
        }
        String this$funcion = this.getFuncion();
        String other$funcion = other.getFuncion();
        if (this$funcion == null ? other$funcion != null : !this$funcion.equals(other$funcion)) {
            return false;
        }
        String this$sistema = this.getSistema();
        String other$sistema = other.getSistema();
        return !(this$sistema == null ? other$sistema != null : !this$sistema.equals(other$sistema));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PresupuestoLinea;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Double $m2 = this.getM2();
        result = result * 59 + ($m2 == null ? 43 : ((Object)$m2).hashCode());
        Double $hhPorM2 = this.getHhPorM2();
        result = result * 59 + ($hhPorM2 == null ? 43 : ((Object)$hhPorM2).hashCode());
        Double $precioPorHh = this.getPrecioPorHh();
        result = result * 59 + ($precioPorHh == null ? 43 : ((Object)$precioPorHh).hashCode());
        Double $precioPorHhUsd = this.getPrecioPorHhUsd();
        result = result * 59 + ($precioPorHhUsd == null ? 43 : ((Object)$precioPorHhUsd).hashCode());
        Integer $mes = this.getMes();
        result = result * 59 + ($mes == null ? 43 : ((Object)$mes).hashCode());
        Proyecto $proyecto = this.getProyecto();
        result = result * 59 + ($proyecto == null ? 43 : ((Object)$proyecto).hashCode());
        String $etapa = this.getEtapa();
        result = result * 59 + ($etapa == null ? 43 : $etapa.hashCode());
        String $tipoSuperficie = this.getTipoSuperficie();
        result = result * 59 + ($tipoSuperficie == null ? 43 : $tipoSuperficie.hashCode());
        String $funcion = this.getFuncion();
        result = result * 59 + ($funcion == null ? 43 : $funcion.hashCode());
        String $sistema = this.getSistema();
        result = result * 59 + ($sistema == null ? 43 : $sistema.hashCode());
        return result;
    }

    public String toString() {
        return "PresupuestoLinea(id=" + this.getId() + ", proyecto=" + String.valueOf(this.getProyecto()) + ", etapa=" + this.getEtapa() + ", tipoSuperficie=" + this.getTipoSuperficie() + ", funcion=" + this.getFuncion() + ", sistema=" + this.getSistema() + ", m2=" + this.getM2() + ", hhPorM2=" + this.getHhPorM2() + ", precioPorHh=" + this.getPrecioPorHh() + ", precioPorHhUsd=" + this.getPrecioPorHhUsd() + ", mes=" + this.getMes() + ")";
    }

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

        public String toString() {
            return "PresupuestoLinea.PresupuestoLineaBuilder(id=" + this.id + ", proyecto=" + String.valueOf(this.proyecto) + ", etapa=" + this.etapa + ", tipoSuperficie=" + this.tipoSuperficie + ", funcion=" + this.funcion + ", sistema=" + this.sistema + ", m2=" + this.m2 + ", hhPorM2=" + this.hhPorM2 + ", precioPorHh=" + this.precioPorHh + ", precioPorHhUsd=" + this.precioPorHhUsd + ", mes=" + this.mes + ")";
        }
    }
}
