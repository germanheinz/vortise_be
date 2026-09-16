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
@Table(name = "catalogos_empresa")
public class CatalogoEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    private String tipo;
    private String codigo;
    private String nombre;
    protected CatalogoEmpresa() { }
    public CatalogoEmpresa(Empresa empresa, String tipo, String codigo, String nombre) { this.empresa = empresa; this.tipo = tipo; this.codigo = codigo; this.nombre = nombre; }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getTipo() { return tipo; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
