/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
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
    @Column(nullable=false)
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
        if (o == this) {
            return true;
        }
        if (!(o instanceof Rendimiento)) {
            return false;
        }
        Rendimiento other = (Rendimiento)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
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
        String this$fase = this.getFase();
        String other$fase = other.getFase();
        if (this$fase == null ? other$fase != null : !this$fase.equals(other$fase)) {
            return false;
        }
        String this$etapa = this.getEtapa();
        String other$etapa = other.getEtapa();
        if (this$etapa == null ? other$etapa != null : !this$etapa.equals(other$etapa)) {
            return false;
        }
        String this$sistema = this.getSistema();
        String other$sistema = other.getSistema();
        if (this$sistema == null ? other$sistema != null : !this$sistema.equals(other$sistema)) {
            return false;
        }
        String this$rol = this.getRol();
        String other$rol = other.getRol();
        return !(this$rol == null ? other$rol != null : !this$rol.equals(other$rol));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Rendimiento;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Double $hhPorM2 = this.getHhPorM2();
        result = result * 59 + ($hhPorM2 == null ? 43 : ((Object)$hhPorM2).hashCode());
        Double $precioPorHh = this.getPrecioPorHh();
        result = result * 59 + ($precioPorHh == null ? 43 : ((Object)$precioPorHh).hashCode());
        String $fase = this.getFase();
        result = result * 59 + ($fase == null ? 43 : $fase.hashCode());
        String $etapa = this.getEtapa();
        result = result * 59 + ($etapa == null ? 43 : $etapa.hashCode());
        String $sistema = this.getSistema();
        result = result * 59 + ($sistema == null ? 43 : $sistema.hashCode());
        String $rol = this.getRol();
        result = result * 59 + ($rol == null ? 43 : $rol.hashCode());
        return result;
    }

    public String toString() {
        return "Rendimiento(id=" + this.getId() + ", fase=" + this.getFase() + ", etapa=" + this.getEtapa() + ", sistema=" + this.getSistema() + ", rol=" + this.getRol() + ", hhPorM2=" + this.getHhPorM2() + ", precioPorHh=" + this.getPrecioPorHh() + ")";
    }

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

        public String toString() {
            return "Rendimiento.RendimientoBuilder(id=" + this.id + ", fase=" + this.fase + ", etapa=" + this.etapa + ", sistema=" + this.sistema + ", rol=" + this.rol + ", hhPorM2=" + this.hhPorM2 + ", precioPorHh=" + this.precioPorHh + ")";
        }
    }
}
