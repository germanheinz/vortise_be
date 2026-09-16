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
@Table(name = "rubros_obra")
public class RubroObra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    private String nRubro;
    private String rubro;
    private String nivel;
    private Integer subNumeroRubro;
    private String subRubro;
    private Double cantidad;
    private String unidad;
    private Double productividad;
    private String empresa;
    private String tipoContratista = "PROPIA";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcontratista_id")
    private CatalogoEmpresa subcontratista;
    private Double horasOficialesPrevistas;
    private Double horasAyudantesPrevistas;
    private Integer cantidadPersonas;
    private LocalDate fechaInicioPlanificada;
    private LocalDate fechaFinPlanificada;
    private boolean cronogramaManual;

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

    public String getnRubro() {
        return nRubro;
    }

    public void setnRubro(String nRubro) {
        this.nRubro = nRubro;
    }

    public String getRubro() {
        return rubro;
    }

    public String getNivel() {
        return nivel;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public Integer getSubNumeroRubro() {
        return subNumeroRubro;
    }

    public void setSubNumeroRubro(Integer subNumeroRubro) {
        this.subNumeroRubro = subNumeroRubro;
    }

    public String getSubRubro() {
        return subRubro;
    }

    public void setSubRubro(String subRubro) {
        this.subRubro = subRubro;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Double getProductividad() {
        return productividad;
    }

    public void setProductividad(Double productividad) {
        this.productividad = productividad;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getTipoContratista() {
        return tipoContratista;
    }

    public void setTipoContratista(String tipoContratista) {
        this.tipoContratista = tipoContratista;
    }

    public CatalogoEmpresa getSubcontratista() {
        return subcontratista;
    }

    public void setSubcontratista(CatalogoEmpresa subcontratista) {
        this.subcontratista = subcontratista;
    }

    public Double getHorasOficialesPrevistas() {
        return horasOficialesPrevistas;
    }

    public void setHorasOficialesPrevistas(Double horasOficialesPrevistas) {
        this.horasOficialesPrevistas = horasOficialesPrevistas;
    }

    public Double getHorasAyudantesPrevistas() {
        return horasAyudantesPrevistas;
    }

    public void setHorasAyudantesPrevistas(Double horasAyudantesPrevistas) {
        this.horasAyudantesPrevistas = horasAyudantesPrevistas;
    }

    public Integer getCantidadPersonas() {
        return cantidadPersonas;
    }

    public void setCantidadPersonas(Integer cantidadPersonas) {
        this.cantidadPersonas = cantidadPersonas;
    }

    public LocalDate getFechaInicioPlanificada() { return fechaInicioPlanificada; }
    public void setFechaInicioPlanificada(LocalDate fechaInicioPlanificada) { this.fechaInicioPlanificada = fechaInicioPlanificada; }
    public LocalDate getFechaFinPlanificada() { return fechaFinPlanificada; }
    public void setFechaFinPlanificada(LocalDate fechaFinPlanificada) { this.fechaFinPlanificada = fechaFinPlanificada; }
    public boolean isCronogramaManual() { return cronogramaManual; }
    public void setCronogramaManual(boolean cronogramaManual) { this.cronogramaManual = cronogramaManual; }
}