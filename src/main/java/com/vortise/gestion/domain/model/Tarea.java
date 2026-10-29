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
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
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
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        Tarea other = (Tarea) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof Tarea;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "Tarea(id=" + getId() + ")"; }

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

        public String toString() { return "TareaBuilder"; }
    }
}
