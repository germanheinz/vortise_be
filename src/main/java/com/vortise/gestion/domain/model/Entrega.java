package com.vortise.gestion.domain.model;

import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.Tarea;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="entregas")
public class Entrega {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="proyecto_id", nullable=false)
    private Proyecto proyecto;
    @Column(nullable=false)
    private String nombre;
    private Integer orden;
    @OneToMany(mappedBy="entrega", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<Tarea> tareas;

    private static List<Tarea> $default$tareas() {
        return new ArrayList<Tarea>();
    }

    public static EntregaBuilder builder() {
        return new EntregaBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public Proyecto getProyecto() {
        return this.proyecto;
    }

    public String getNombre() {
        return this.nombre;
    }

    public Integer getOrden() {
        return this.orden;
    }

    public List<Tarea> getTareas() {
        return this.tareas;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public void setTareas(List<Tarea> tareas) {
        this.tareas = tareas;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        Entrega other = (Entrega) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof Entrega;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "Entrega(id=" + getId() + ")"; }

    public Entrega() {
        this.tareas = Entrega.$default$tareas();
    }

    public Entrega(Long id, Proyecto proyecto, String nombre, Integer orden, List<Tarea> tareas) {
        this.id = id;
        this.proyecto = proyecto;
        this.nombre = nombre;
        this.orden = orden;
        this.tareas = tareas;
    }

    public static class EntregaBuilder {
        private Long id;
        private Proyecto proyecto;
        private String nombre;
        private Integer orden;
        private boolean tareas$set;
        private List<Tarea> tareas$value;

        EntregaBuilder() {
        }

        public EntregaBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EntregaBuilder proyecto(Proyecto proyecto) {
            this.proyecto = proyecto;
            return this;
        }

        public EntregaBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public EntregaBuilder orden(Integer orden) {
            this.orden = orden;
            return this;
        }

        public EntregaBuilder tareas(List<Tarea> tareas) {
            this.tareas$value = tareas;
            this.tareas$set = true;
            return this;
        }

        public Entrega build() {
            List<Tarea> tareas$value = this.tareas$value;
            if (!this.tareas$set) {
                tareas$value = Entrega.$default$tareas();
            }
            return new Entrega(this.id, this.proyecto, this.nombre, this.orden, tareas$value);
        }

        public String toString() { return "EntregaBuilder"; }
    }
}
