package com.vortise.gestion.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "responsables_empresa")
public class ResponsableEmpresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private String rol;
    private String nombre;
    private String email;
    private String telefono;
    @Column(length = 120)
    private String area;

    protected ResponsableEmpresa() {
    }

    public ResponsableEmpresa(Empresa empresa, String rol, String nombre, String email, String telefono) {
        this(empresa, rol, nombre, email, telefono, null);
    }

    public ResponsableEmpresa(Empresa empresa, String rol, String nombre, String email, String telefono, String area) {
        this.empresa = empresa;
        this.rol = rol;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.area = area;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getRol() { return rol; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getArea() { return area; }
    public void setRol(String rol) { this.rol = rol; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setArea(String area) { this.area = area; }
}
