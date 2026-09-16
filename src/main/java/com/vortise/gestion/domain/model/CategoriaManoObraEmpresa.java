package com.vortise.gestion.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias_mano_obra_empresa")
public class CategoriaManoObraEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    private String nombre;
    private Double costoHora;
    private boolean habilitada;

    protected CategoriaManoObraEmpresa() { }
    public CategoriaManoObraEmpresa(Empresa empresa, String nombre, Double costoHora, boolean habilitada) {
        this.empresa = empresa; this.nombre = nombre; this.costoHora = costoHora; this.habilitada = habilitada;
    }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getNombre() { return nombre; }
    public Double getCostoHora() { return costoHora; }
    public boolean isHabilitada() { return habilitada; }
    public void setCostoHora(Double costoHora) { this.costoHora = costoHora; }
    public void setHabilitada(boolean habilitada) { this.habilitada = habilitada; }
}
