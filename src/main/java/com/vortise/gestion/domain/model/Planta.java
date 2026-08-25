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
@Table(name="plantas")
public class Planta {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="proyecto_id", nullable=false)
    private Proyecto proyecto;
    @Column(nullable=false)
    private String nivel;
    private Double superficie;
    private Double supCubierta;
    private Double supDescubierta;
    private Double supCalculada;
    private Double coefPlantaTipo;

    public static PlantaBuilder builder() {
        return new PlantaBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public Proyecto getProyecto() {
        return this.proyecto;
    }

    public String getNivel() {
        return this.nivel;
    }

    public Double getSuperficie() {
        return this.superficie;
    }

    public Double getSupCubierta() {
        return this.supCubierta;
    }

    public Double getSupDescubierta() {
        return this.supDescubierta;
    }

    public Double getSupCalculada() {
        return this.supCalculada;
    }

    public Double getCoefPlantaTipo() {
        return this.coefPlantaTipo;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public void setSuperficie(Double superficie) {
        this.superficie = superficie;
    }

    public void setSupCubierta(Double supCubierta) {
        this.supCubierta = supCubierta;
    }

    public void setSupDescubierta(Double supDescubierta) {
        this.supDescubierta = supDescubierta;
    }

    public void setSupCalculada(Double supCalculada) {
        this.supCalculada = supCalculada;
    }

    public void setCoefPlantaTipo(Double coefPlantaTipo) {
        this.coefPlantaTipo = coefPlantaTipo;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Planta)) {
            return false;
        }
        Planta other = (Planta)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Double this$superficie = this.getSuperficie();
        Double other$superficie = other.getSuperficie();
        if (this$superficie == null ? other$superficie != null : !((Object)this$superficie).equals(other$superficie)) {
            return false;
        }
        Double this$supCubierta = this.getSupCubierta();
        Double other$supCubierta = other.getSupCubierta();
        if (this$supCubierta == null ? other$supCubierta != null : !((Object)this$supCubierta).equals(other$supCubierta)) {
            return false;
        }
        Double this$supDescubierta = this.getSupDescubierta();
        Double other$supDescubierta = other.getSupDescubierta();
        if (this$supDescubierta == null ? other$supDescubierta != null : !((Object)this$supDescubierta).equals(other$supDescubierta)) {
            return false;
        }
        Double this$supCalculada = this.getSupCalculada();
        Double other$supCalculada = other.getSupCalculada();
        if (this$supCalculada == null ? other$supCalculada != null : !((Object)this$supCalculada).equals(other$supCalculada)) {
            return false;
        }
        Double this$coefPlantaTipo = this.getCoefPlantaTipo();
        Double other$coefPlantaTipo = other.getCoefPlantaTipo();
        if (this$coefPlantaTipo == null ? other$coefPlantaTipo != null : !((Object)this$coefPlantaTipo).equals(other$coefPlantaTipo)) {
            return false;
        }
        Proyecto this$proyecto = this.getProyecto();
        Proyecto other$proyecto = other.getProyecto();
        if (this$proyecto == null ? other$proyecto != null : !((Object)this$proyecto).equals(other$proyecto)) {
            return false;
        }
        String this$nivel = this.getNivel();
        String other$nivel = other.getNivel();
        return !(this$nivel == null ? other$nivel != null : !this$nivel.equals(other$nivel));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Planta;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Double $superficie = this.getSuperficie();
        result = result * 59 + ($superficie == null ? 43 : ((Object)$superficie).hashCode());
        Double $supCubierta = this.getSupCubierta();
        result = result * 59 + ($supCubierta == null ? 43 : ((Object)$supCubierta).hashCode());
        Double $supDescubierta = this.getSupDescubierta();
        result = result * 59 + ($supDescubierta == null ? 43 : ((Object)$supDescubierta).hashCode());
        Double $supCalculada = this.getSupCalculada();
        result = result * 59 + ($supCalculada == null ? 43 : ((Object)$supCalculada).hashCode());
        Double $coefPlantaTipo = this.getCoefPlantaTipo();
        result = result * 59 + ($coefPlantaTipo == null ? 43 : ((Object)$coefPlantaTipo).hashCode());
        Proyecto $proyecto = this.getProyecto();
        result = result * 59 + ($proyecto == null ? 43 : ((Object)$proyecto).hashCode());
        String $nivel = this.getNivel();
        result = result * 59 + ($nivel == null ? 43 : $nivel.hashCode());
        return result;
    }

    public String toString() {
        return "Planta(id=" + this.getId() + ", proyecto=" + String.valueOf(this.getProyecto()) + ", nivel=" + this.getNivel() + ", superficie=" + this.getSuperficie() + ", supCubierta=" + this.getSupCubierta() + ", supDescubierta=" + this.getSupDescubierta() + ", supCalculada=" + this.getSupCalculada() + ", coefPlantaTipo=" + this.getCoefPlantaTipo() + ")";
    }

    public Planta() {
    }

    public Planta(Long id, Proyecto proyecto, String nivel, Double superficie, Double supCubierta, Double supDescubierta, Double supCalculada, Double coefPlantaTipo) {
        this.id = id;
        this.proyecto = proyecto;
        this.nivel = nivel;
        this.superficie = superficie;
        this.supCubierta = supCubierta;
        this.supDescubierta = supDescubierta;
        this.supCalculada = supCalculada;
        this.coefPlantaTipo = coefPlantaTipo;
    }

    public static class PlantaBuilder {
        private Long id;
        private Proyecto proyecto;
        private String nivel;
        private Double superficie;
        private Double supCubierta;
        private Double supDescubierta;
        private Double supCalculada;
        private Double coefPlantaTipo;

        PlantaBuilder() {
        }

        public PlantaBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PlantaBuilder proyecto(Proyecto proyecto) {
            this.proyecto = proyecto;
            return this;
        }

        public PlantaBuilder nivel(String nivel) {
            this.nivel = nivel;
            return this;
        }

        public PlantaBuilder superficie(Double superficie) {
            this.superficie = superficie;
            return this;
        }

        public PlantaBuilder supCubierta(Double supCubierta) {
            this.supCubierta = supCubierta;
            return this;
        }

        public PlantaBuilder supDescubierta(Double supDescubierta) {
            this.supDescubierta = supDescubierta;
            return this;
        }

        public PlantaBuilder supCalculada(Double supCalculada) {
            this.supCalculada = supCalculada;
            return this;
        }

        public PlantaBuilder coefPlantaTipo(Double coefPlantaTipo) {
            this.coefPlantaTipo = coefPlantaTipo;
            return this;
        }

        public Planta build() {
            return new Planta(this.id, this.proyecto, this.nivel, this.superficie, this.supCubierta, this.supDescubierta, this.supCalculada, this.coefPlantaTipo);
        }

        public String toString() {
            return "Planta.PlantaBuilder(id=" + this.id + ", proyecto=" + String.valueOf(this.proyecto) + ", nivel=" + this.nivel + ", superficie=" + this.superficie + ", supCubierta=" + this.supCubierta + ", supDescubierta=" + this.supDescubierta + ", supCalculada=" + this.supCalculada + ", coefPlantaTipo=" + this.coefPlantaTipo + ")";
        }
    }
}
