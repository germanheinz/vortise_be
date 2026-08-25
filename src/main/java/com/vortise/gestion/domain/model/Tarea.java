/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 */
package com.vortise.gestion.domain.model;

import com.vortise.gestion.domain.model.Entrega;
import com.vortise.gestion.domain.model.StatusTarea;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name="tareas")
public class Tarea {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="entrega_id", nullable=false)
    private Entrega entrega;
    private String fase;
    private String etapa;
    private String sistema;
    @Column(nullable=false)
    private String nombre;
    @Enumerated(value=EnumType.STRING)
    private StatusTarea status;
    private Integer avancePct;
    private Double cantidad;
    private Double costo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer mes;

    private static StatusTarea $default$status() {
        return StatusTarea.PENDIENTE;
    }

    public static TareaBuilder builder() {
        return new TareaBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public Entrega getEntrega() {
        return this.entrega;
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

    public String getNombre() {
        return this.nombre;
    }

    public StatusTarea getStatus() {
        return this.status;
    }

    public Integer getAvancePct() {
        return this.avancePct;
    }

    public Double getCantidad() {
        return this.cantidad;
    }

    public Double getCosto() {
        return this.costo;
    }

    public LocalDate getFechaInicio() {
        return this.fechaInicio;
    }

    public LocalDate getFechaFin() {
        return this.fechaFin;
    }

    public Integer getMes() {
        return this.mes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEntrega(Entrega entrega) {
        this.entrega = entrega;
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

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setStatus(StatusTarea status) {
        this.status = status;
    }

    public void setAvancePct(Integer avancePct) {
        this.avancePct = avancePct;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Tarea)) {
            return false;
        }
        Tarea other = (Tarea)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$avancePct = this.getAvancePct();
        Integer other$avancePct = other.getAvancePct();
        if (this$avancePct == null ? other$avancePct != null : !((Object)this$avancePct).equals(other$avancePct)) {
            return false;
        }
        Double this$cantidad = this.getCantidad();
        Double other$cantidad = other.getCantidad();
        if (this$cantidad == null ? other$cantidad != null : !((Object)this$cantidad).equals(other$cantidad)) {
            return false;
        }
        Double this$costo = this.getCosto();
        Double other$costo = other.getCosto();
        if (this$costo == null ? other$costo != null : !((Object)this$costo).equals(other$costo)) {
            return false;
        }
        Integer this$mes = this.getMes();
        Integer other$mes = other.getMes();
        if (this$mes == null ? other$mes != null : !((Object)this$mes).equals(other$mes)) {
            return false;
        }
        Entrega this$entrega = this.getEntrega();
        Entrega other$entrega = other.getEntrega();
        if (this$entrega == null ? other$entrega != null : !((Object)this$entrega).equals(other$entrega)) {
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
        String this$nombre = this.getNombre();
        String other$nombre = other.getNombre();
        if (this$nombre == null ? other$nombre != null : !this$nombre.equals(other$nombre)) {
            return false;
        }
        StatusTarea this$status = this.getStatus();
        StatusTarea other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)((Object)this$status)).equals((Object)other$status)) {
            return false;
        }
        LocalDate this$fechaInicio = this.getFechaInicio();
        LocalDate other$fechaInicio = other.getFechaInicio();
        if (this$fechaInicio == null ? other$fechaInicio != null : !((Object)this$fechaInicio).equals(other$fechaInicio)) {
            return false;
        }
        LocalDate this$fechaFin = this.getFechaFin();
        LocalDate other$fechaFin = other.getFechaFin();
        return !(this$fechaFin == null ? other$fechaFin != null : !((Object)this$fechaFin).equals(other$fechaFin));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Tarea;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $avancePct = this.getAvancePct();
        result = result * 59 + ($avancePct == null ? 43 : ((Object)$avancePct).hashCode());
        Double $cantidad = this.getCantidad();
        result = result * 59 + ($cantidad == null ? 43 : ((Object)$cantidad).hashCode());
        Double $costo = this.getCosto();
        result = result * 59 + ($costo == null ? 43 : ((Object)$costo).hashCode());
        Integer $mes = this.getMes();
        result = result * 59 + ($mes == null ? 43 : ((Object)$mes).hashCode());
        Entrega $entrega = this.getEntrega();
        result = result * 59 + ($entrega == null ? 43 : ((Object)$entrega).hashCode());
        String $fase = this.getFase();
        result = result * 59 + ($fase == null ? 43 : $fase.hashCode());
        String $etapa = this.getEtapa();
        result = result * 59 + ($etapa == null ? 43 : $etapa.hashCode());
        String $sistema = this.getSistema();
        result = result * 59 + ($sistema == null ? 43 : $sistema.hashCode());
        String $nombre = this.getNombre();
        result = result * 59 + ($nombre == null ? 43 : $nombre.hashCode());
        StatusTarea $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)((Object)$status)).hashCode());
        LocalDate $fechaInicio = this.getFechaInicio();
        result = result * 59 + ($fechaInicio == null ? 43 : ((Object)$fechaInicio).hashCode());
        LocalDate $fechaFin = this.getFechaFin();
        result = result * 59 + ($fechaFin == null ? 43 : ((Object)$fechaFin).hashCode());
        return result;
    }

    public String toString() {
        return "Tarea(id=" + this.getId() + ", entrega=" + String.valueOf(this.getEntrega()) + ", fase=" + this.getFase() + ", etapa=" + this.getEtapa() + ", sistema=" + this.getSistema() + ", nombre=" + this.getNombre() + ", status=" + String.valueOf((Object)this.getStatus()) + ", avancePct=" + this.getAvancePct() + ", cantidad=" + this.getCantidad() + ", costo=" + this.getCosto() + ", fechaInicio=" + String.valueOf(this.getFechaInicio()) + ", fechaFin=" + String.valueOf(this.getFechaFin()) + ", mes=" + this.getMes() + ")";
    }

    public Tarea() {
        this.status = Tarea.$default$status();
    }

    public Tarea(Long id, Entrega entrega, String fase, String etapa, String sistema, String nombre, StatusTarea status, Integer avancePct, Double cantidad, Double costo, LocalDate fechaInicio, LocalDate fechaFin, Integer mes) {
        this.id = id;
        this.entrega = entrega;
        this.fase = fase;
        this.etapa = etapa;
        this.sistema = sistema;
        this.nombre = nombre;
        this.status = status;
        this.avancePct = avancePct;
        this.cantidad = cantidad;
        this.costo = costo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.mes = mes;
    }

    public static class TareaBuilder {
        private Long id;
        private Entrega entrega;
        private String fase;
        private String etapa;
        private String sistema;
        private String nombre;
        private boolean status$set;
        private StatusTarea status$value;
        private Integer avancePct;
        private Double cantidad;
        private Double costo;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Integer mes;

        TareaBuilder() {
        }

        public TareaBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TareaBuilder entrega(Entrega entrega) {
            this.entrega = entrega;
            return this;
        }

        public TareaBuilder fase(String fase) {
            this.fase = fase;
            return this;
        }

        public TareaBuilder etapa(String etapa) {
            this.etapa = etapa;
            return this;
        }

        public TareaBuilder sistema(String sistema) {
            this.sistema = sistema;
            return this;
        }

        public TareaBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public TareaBuilder status(StatusTarea status) {
            this.status$value = status;
            this.status$set = true;
            return this;
        }

        public TareaBuilder avancePct(Integer avancePct) {
            this.avancePct = avancePct;
            return this;
        }

        public TareaBuilder cantidad(Double cantidad) {
            this.cantidad = cantidad;
            return this;
        }

        public TareaBuilder costo(Double costo) {
            this.costo = costo;
            return this;
        }

        public TareaBuilder fechaInicio(LocalDate fechaInicio) {
            this.fechaInicio = fechaInicio;
            return this;
        }

        public TareaBuilder fechaFin(LocalDate fechaFin) {
            this.fechaFin = fechaFin;
            return this;
        }

        public TareaBuilder mes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public Tarea build() {
            StatusTarea status$value = this.status$value;
            if (!this.status$set) {
                status$value = Tarea.$default$status();
            }
            return new Tarea(this.id, this.entrega, this.fase, this.etapa, this.sistema, this.nombre, status$value, this.avancePct, this.cantidad, this.costo, this.fechaInicio, this.fechaFin, this.mes);
        }

        public String toString() {
            return "Tarea.TareaBuilder(id=" + this.id + ", entrega=" + String.valueOf(this.entrega) + ", fase=" + this.fase + ", etapa=" + this.etapa + ", sistema=" + this.sistema + ", nombre=" + this.nombre + ", status$value=" + String.valueOf((Object)this.status$value) + ", avancePct=" + this.avancePct + ", cantidad=" + this.cantidad + ", costo=" + this.costo + ", fechaInicio=" + String.valueOf(this.fechaInicio) + ", fechaFin=" + String.valueOf(this.fechaFin) + ", mes=" + this.mes + ")";
        }
    }
}
