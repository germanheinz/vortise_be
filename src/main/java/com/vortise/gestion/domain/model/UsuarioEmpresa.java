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
import java.time.Instant;

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
    @Column(nullable = false) private boolean superAdmin = false;
    @Column(name = "ultimo_login_exitoso_en", nullable = true) private Instant ultimoLoginExitosoEn;
    private static final java.security.SecureRandom SESSION_GENERATIONS = new java.security.SecureRandom();
    @Column(nullable = false) private long sessionVersion = nuevaGeneracionSesion();
    @jakarta.persistence.Version private long persistenceVersion;

    protected UsuarioEmpresa() { }
    public UsuarioEmpresa(Empresa empresa, String email, String nombre, String passwordHash, RolEmpresa rol) { this.empresa = empresa; this.email = email; this.nombre = nombre; this.passwordHash = passwordHash; this.rol = rol; }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public RolEmpresa getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public boolean isSuperAdmin() { return superAdmin; }
    public Instant getUltimoLoginExitosoEn() { return ultimoLoginExitosoEn; }
    public long getSessionVersion() { return sessionVersion; }
    /** New identity/restored identity must not reuse generations from previously signed JWTs. */
    public static long nuevaGeneracionSesion() { return SESSION_GENERATIONS.nextLong(1, Long.MAX_VALUE / 2); }
    public void setUltimoLoginExitosoEn(Instant value) { ultimoLoginExitosoEn = value; }
    public void setSuperAdmin(boolean value) { if (superAdmin != value) sessionVersion++; superAdmin = value; }
    public void setNombre(String value) { nombre = value; }
    public void setPasswordHash(String value) { sessionVersion++; passwordHash = value; }
    public void setRol(RolEmpresa value) { if (rol != value) sessionVersion++; rol = value; }
    public void setActivo(boolean value) { if (activo != value) sessionVersion++; activo = value; }
}
