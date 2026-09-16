package com.vortise.gestion.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "obra_tareas")
public class ObraTarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    private String nombre;
    private String sector;
    private String nivel;
    private String unidad;
    private String rubro;
    private Double porcentajeCumplimiento;
    private String cumplimiento;
    private String causaNoCumplimiento;
    private String comentario;
    private Double horasOficiales;
    private Double horasAyudantes;
    private Double cantidadPrevista;
    private Double cantidadReal;
    private String medidaCorrectiva;
    private String descripcion;
    private LocalDate fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public Double getPorcentajeCumplimiento() {
        return porcentajeCumplimiento;
    }

    public void setPorcentajeCumplimiento(Double porcentajeCumplimiento) {
        this.porcentajeCumplimiento = porcentajeCumplimiento;
    }

    public String getCumplimiento() {
        return cumplimiento;
    }

    public void setCumplimiento(String cumplimiento) {
        this.cumplimiento = cumplimiento;
    }

    public String getCausaNoCumplimiento() {
        return causaNoCumplimiento;
    }

    public void setCausaNoCumplimiento(String causaNoCumplimiento) {
        this.causaNoCumplimiento = causaNoCumplimiento;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Double getHorasOficiales() { return horasOficiales; }
    public void setHorasOficiales(Double horasOficiales) { this.horasOficiales = horasOficiales; }
    public Double getHorasAyudantes() { return horasAyudantes; }
    public void setHorasAyudantes(Double horasAyudantes) { this.horasAyudantes = horasAyudantes; }
    public Double getCantidadPrevista() { return cantidadPrevista; }
    public void setCantidadPrevista(Double cantidadPrevista) { this.cantidadPrevista = cantidadPrevista; }
    public Double getCantidadReal() { return cantidadReal; }
    public void setCantidadReal(Double cantidadReal) { this.cantidadReal = cantidadReal; }
    public String getMedidaCorrectiva() { return medidaCorrectiva; }
    public void setMedidaCorrectiva(String medidaCorrectiva) { this.medidaCorrectiva = medidaCorrectiva; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}