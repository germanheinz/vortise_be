/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.OneToMany
 *  jakarta.persistence.Table
 */
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
        if (o == this) {
            return true;
        }
        if (!(o instanceof Entrega)) {
            return false;
        }
        Entrega other = (Entrega)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$orden = this.getOrden();
        Integer other$orden = other.getOrden();
        if (this$orden == null ? other$orden != null : !((Object)this$orden).equals(other$orden)) {
            return false;
        }
        Proyecto this$proyecto = this.getProyecto();
        Proyecto other$proyecto = other.getProyecto();
        if (this$proyecto == null ? other$proyecto != null : !((Object)this$proyecto).equals(other$proyecto)) {
            return false;
        }
        String this$nombre = this.getNombre();
        String other$nombre = other.getNombre();
        if (this$nombre == null ? other$nombre != null : !this$nombre.equals(other$nombre)) {
            return false;
        }
        List<Tarea> this$tareas = this.getTareas();
        List<Tarea> other$tareas = other.getTareas();
        return !(this$tareas == null ? other$tareas != null : !((Object)this$tareas).equals(other$tareas));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Entrega;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $orden = this.getOrden();
        result = result * 59 + ($orden == null ? 43 : ((Object)$orden).hashCode());
        Proyecto $proyecto = this.getProyecto();
        result = result * 59 + ($proyecto == null ? 43 : ((Object)$proyecto).hashCode());
        String $nombre = this.getNombre();
        result = result * 59 + ($nombre == null ? 43 : $nombre.hashCode());
        List<Tarea> $tareas = this.getTareas();
        result = result * 59 + ($tareas == null ? 43 : ((Object)$tareas).hashCode());
        return result;
    }

    public String toString() {
        return "Entrega(id=" + this.getId() + ", proyecto=" + String.valueOf(this.getProyecto()) + ", nombre=" + this.getNombre() + ", orden=" + this.getOrden() + ", tareas=" + String.valueOf(this.getTareas()) + ")";
    }

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

        public String toString() {
            return "Entrega.EntregaBuilder(id=" + this.id + ", proyecto=" + String.valueOf(this.proyecto) + ", nombre=" + this.nombre + ", orden=" + this.orden + ", tareas$value=" + String.valueOf(this.tareas$value) + ")";
        }
    }
}
