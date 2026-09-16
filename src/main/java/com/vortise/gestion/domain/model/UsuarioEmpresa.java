package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios_empresa")
public class UsuarioEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RolEmpresa rol;
    @Column(nullable = false) private boolean activo = true;

    protected UsuarioEmpresa() { }
    public UsuarioEmpresa(Empresa empresa, String email, String nombre, String passwordHash, RolEmpresa rol) { this.empresa = empresa; this.email = email; this.nombre = nombre; this.passwordHash = passwordHash; this.rol = rol; }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public RolEmpresa getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public void setNombre(String value) { nombre = value; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public void setRol(RolEmpresa value) { rol = value; }
    public void setActivo(boolean value) { activo = value; }
}
