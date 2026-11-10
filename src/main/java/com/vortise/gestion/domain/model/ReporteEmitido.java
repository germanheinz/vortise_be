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

/** Immutable record of an issued report: the computed data is stored as a JSON snapshot. */
@Entity
@Table(name = "reportes_emitidos")
public class ReporteEmitido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private LocalDate semanaDesde;

    @Column(nullable = false)
    private LocalDate semanaHasta;

    @Column(nullable = false)
    private LocalDate fechaCorte;

    private String emitidoPor;
    private Long emitidoPorUsuarioId;

    @Column(nullable = false)
    private LocalDateTime emitidoEn;

    @Column(nullable = false)
    private Integer versionEsquema;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshot;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public LocalDate getSemanaDesde() { return semanaDesde; }
    public void setSemanaDesde(LocalDate semanaDesde) { this.semanaDesde = semanaDesde; }
    public LocalDate getSemanaHasta() { return semanaHasta; }
    public void setSemanaHasta(LocalDate semanaHasta) { this.semanaHasta = semanaHasta; }
    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
    public String getEmitidoPor() { return emitidoPor; }
    public void setEmitidoPor(String emitidoPor) { this.emitidoPor = emitidoPor; }
    public Long getEmitidoPorUsuarioId() { return emitidoPorUsuarioId; }
    public void setEmitidoPorUsuarioId(Long emitidoPorUsuarioId) { this.emitidoPorUsuarioId = emitidoPorUsuarioId; }
    public LocalDateTime getEmitidoEn() { return emitidoEn; }
    public void setEmitidoEn(LocalDateTime emitidoEn) { this.emitidoEn = emitidoEn; }
    public Integer getVersionEsquema() { return versionEsquema; }
    public void setVersionEsquema(Integer versionEsquema) { this.versionEsquema = versionEsquema; }
    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
}
