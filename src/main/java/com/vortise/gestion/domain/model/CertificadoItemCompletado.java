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
import java.time.LocalDateTime;

/** Activity closed at 100% by the project manager although less was loaded; it takes no further loads. */
@Entity
@Table(name = "certificado_items_completados")
public class CertificadoItemCompletado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificado_id", nullable = false)
    private CertificadoObra certificado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubro_id", nullable = false)
    private RubroObra rubro;

    private Long marcadoPorUsuarioId;

    @Column(nullable = false)
    private LocalDateTime marcadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CertificadoObra getCertificado() { return certificado; }
    public void setCertificado(CertificadoObra certificado) { this.certificado = certificado; }
    public RubroObra getRubro() { return rubro; }
    public void setRubro(RubroObra rubro) { this.rubro = rubro; }
    public Long getRubroId() { return rubro == null ? null : rubro.getId(); }
    public Long getMarcadoPorUsuarioId() { return marcadoPorUsuarioId; }
    public void setMarcadoPorUsuarioId(Long marcadoPorUsuarioId) { this.marcadoPorUsuarioId = marcadoPorUsuarioId; }
    public LocalDateTime getMarcadoEn() { return marcadoEn; }
    public void setMarcadoEn(LocalDateTime marcadoEn) { this.marcadoEn = marcadoEn; }
}
