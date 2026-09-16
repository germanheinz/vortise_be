package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "registros_horas")
public class RegistroHoras {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    private String numeroRubro;
    private String rubro;
    private String subRubro;
    private String responsable;
    private String etapa;
    private Double cantidadPrevista;
    private Double cantidadReal;
    private Double horasPrevistas;
    private Double horasOficiales;
    private Double horasAyudantes;
    private Double productividadPresupuesto;
    private String causaNoCumplimiento;
    private String medidaCorrectiva;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fin;

    @Column(nullable = false)
    private Double horas;

    private String descripcion;

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

    public String getNumeroRubro() {
        return numeroRubro;
    }

    public void setNumeroRubro(String numeroRubro) {
        this.numeroRubro = numeroRubro;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public String getSubRubro() {
        return subRubro;
    }

    public void setSubRubro(String subRubro) {
        this.subRubro = subRubro;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public Double getCantidadPrevista() {
        return cantidadPrevista;
    }

    public void setCantidadPrevista(Double cantidadPrevista) {
        this.cantidadPrevista = cantidadPrevista;
    }

    public Double getCantidadReal() {
        return cantidadReal;
    }

    public void setCantidadReal(Double cantidadReal) {
        this.cantidadReal = cantidadReal;
    }

    public Double getHorasPrevistas() {
        return horasPrevistas;
    }

    public void setHorasPrevistas(Double horasPrevistas) {
        this.horasPrevistas = horasPrevistas;
    }

    public Double getHorasOficiales() {
        return horasOficiales;
    }

    public void setHorasOficiales(Double horasOficiales) {
        this.horasOficiales = horasOficiales;
    }

    public Double getHorasAyudantes() {
        return horasAyudantes;
    }

    public void setHorasAyudantes(Double horasAyudantes) {
        this.horasAyudantes = horasAyudantes;
    }

    public Double getProductividadPresupuesto() {
        return productividadPresupuesto;
    }

    public void setProductividadPresupuesto(Double productividadPresupuesto) {
        this.productividadPresupuesto = productividadPresupuesto;
    }

    public String getCausaNoCumplimiento() {
        return causaNoCumplimiento;
    }

    public void setCausaNoCumplimiento(String causaNoCumplimiento) {
        this.causaNoCumplimiento = causaNoCumplimiento;
    }

    public String getMedidaCorrectiva() {
        return medidaCorrectiva;
    }

    public void setMedidaCorrectiva(String medidaCorrectiva) {
        this.medidaCorrectiva = medidaCorrectiva;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public void setInicio(OffsetDateTime inicio) {
        this.inicio = inicio;
    }

    public OffsetDateTime getFin() {
        return fin;
    }

    public void setFin(OffsetDateTime fin) {
        this.fin = fin;
    }

    public Double getHoras() {
        return horas;
    }

    public void setHoras(Double horas) {
        this.horas = horas;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}