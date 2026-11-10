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
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Certificate header of one company in an obra; the measured quantities are always derived from loads. */
@Entity
@Table(name = "certificados_obra")
public class CertificadoObra {
    public static final String ABIERTO = "ABIERTO";
    public static final String CERRADO = "CERRADO";
    public static final String MENSUAL = "MENSUAL";
    public static final String QUINCENAL = "QUINCENAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    /** Null means the own labor of the obra's company. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcontratista_id")
    private CatalogoEmpresa subcontratista;

    @Column(nullable = false, length = 20)
    private String periodicidad;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 100)
    private String etiqueta;

    @Column(nullable = false)
    private LocalDate fechaCorte;

    @Column(nullable = false, length = 20)
    private String estado = ABIERTO;

    private Long creadoPorUsuarioId;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    private Long cerradoPorUsuarioId;
    private LocalDateTime cerradoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public CatalogoEmpresa getSubcontratista() { return subcontratista; }
    public void setSubcontratista(CatalogoEmpresa subcontratista) { this.subcontratista = subcontratista; }
    public Long getSubcontratistaId() { return subcontratista == null ? null : subcontratista.getId(); }
    public String getPeriodicidad() { return periodicidad; }
    public void setPeriodicidad(String periodicidad) { this.periodicidad = periodicidad; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isCerrado() { return CERRADO.equals(estado); }
    public Long getCreadoPorUsuarioId() { return creadoPorUsuarioId; }
    public void setCreadoPorUsuarioId(Long creadoPorUsuarioId) { this.creadoPorUsuarioId = creadoPorUsuarioId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public Long getCerradoPorUsuarioId() { return cerradoPorUsuarioId; }
    public void setCerradoPorUsuarioId(Long cerradoPorUsuarioId) { this.cerradoPorUsuarioId = cerradoPorUsuarioId; }
    public LocalDateTime getCerradoEn() { return cerradoEn; }
    public void setCerradoEn(LocalDateTime cerradoEn) { this.cerradoEn = cerradoEn; }
}
