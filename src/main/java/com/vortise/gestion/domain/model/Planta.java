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
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        Planta other = (Planta) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof Planta;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "Planta(id=" + getId() + ")"; }

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

        public String toString() { return "PlantaBuilder"; }
    }
}
