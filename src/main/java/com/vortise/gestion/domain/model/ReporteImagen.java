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

/** Image hosted in Cloudinary. It is a draft while {@code reporte} is null and becomes part of the report once issued. */
@Entity
@Table(name = "reporte_imagenes")
public class ReporteImagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporte_id")
    private ReporteEmitido reporte;

    @Column(nullable = false, unique = true)
    private String publicId;

    @Column(nullable = false, length = 1000)
    private String url;

    private Integer ancho;
    private Integer alto;
    private Long tamanoBytes;
    private String leyenda;

    @Column(nullable = false)
    private Integer orden;

    private Long subidoPorUsuarioId;

    @Column(nullable = false)
    private LocalDateTime subidoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public ReporteEmitido getReporte() { return reporte; }
    public void setReporte(ReporteEmitido reporte) { this.reporte = reporte; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getAncho() { return ancho; }
    public void setAncho(Integer ancho) { this.ancho = ancho; }
    public Integer getAlto() { return alto; }
    public void setAlto(Integer alto) { this.alto = alto; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }
    public String getLeyenda() { return leyenda; }
    public void setLeyenda(String leyenda) { this.leyenda = leyenda; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public Long getSubidoPorUsuarioId() { return subidoPorUsuarioId; }
    public void setSubidoPorUsuarioId(Long subidoPorUsuarioId) { this.subidoPorUsuarioId = subidoPorUsuarioId; }
    public LocalDateTime getSubidoEn() { return subidoEn; }
    public void setSubidoEn(LocalDateTime subidoEn) { this.subidoEn = subidoEn; }
}
