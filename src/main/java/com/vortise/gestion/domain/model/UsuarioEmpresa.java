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
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RolEmpresa rol;
    @Column(nullable = false) private boolean activo = true;
    @Column(nullable = false) private boolean superAdmin = false;
    @Column(name = "ultimo_login_exitoso_en", nullable = true) private Instant ultimoLoginExitosoEn;
    private static final java.security.SecureRandom SESSION_GENERATIONS = new java.security.SecureRandom();
    @Column(nullable = false) private long sessionVersion = nuevaGeneracionSesion();
    @jakarta.persistence.Version private long persistenceVersion;

    @Column(name = "invitante_id") private Long invitanteId;
    @Column(name = "clerk_user_id", unique = true)
    private String clerkUserId;
    @Column(name = "tiene_login")
    private boolean tieneLogin = false;
    @Column(name = "codigo_invitacion", unique = true)
    private String codigoInvitacion;
    @Column(name = "invitacion_expira_en")
    private Instant invitacionExpiraEn;
    @Column(name = "migrado_a_clerk")
    private boolean migradoAClerk = false;
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "usuario_empresa_accesos", joinColumns = @JoinColumn(name = "usuario_id"),
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"usuario_id", "empresa_id"}))
    @Column(name = "empresa_id", nullable = false)
    private java.util.Set<Long> empresasAsignadas = new java.util.HashSet<>();
    // Obras donde un JEFE_DE_OBRA puede escribir; cualquier cambio invalida las sesiones emitidas.
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "usuario_obra", joinColumns = @JoinColumn(name = "usuario_id"),
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"usuario_id", "proyecto_id"}))
    @Column(name = "proyecto_id", nullable = false)
    private java.util.Set<Long> obrasAsignadas = new java.util.HashSet<>();
    public java.util.Set<Long> getObrasAsignadas() { return java.util.Set.copyOf(obrasAsignadas); }
    public void asignarObras(java.util.Set<Long> obras) {
        java.util.Set<Long> nuevas = obras == null ? new java.util.HashSet<>() : new java.util.HashSet<>(obras);
        if (!nuevas.equals(obrasAsignadas)) sessionVersion++;
        obrasAsignadas = nuevas;
    }
    public Long getInvitanteId() { return invitanteId; }
    public java.util.Set<Long> getEmpresasAsignadas() { return java.util.Set.copyOf(empresasAsignadas); }
    public boolean isColaborador() { return rol == RolEmpresa.COLABORADOR; }
    public void asignarEquipo(Long titularId, java.util.Set<Long> empresas) {
        if (titularId == null || empresas == null || empresas.isEmpty()) throw new IllegalArgumentException("Equipo inválido");
        invitanteId = titularId; empresasAsignadas = new java.util.HashSet<>(empresas); rol = RolEmpresa.COLABORADOR;
    }

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
    public String getClerkUserId() { return clerkUserId; }
    public boolean getTieneLogin() { return tieneLogin; }
    public String getCodigoInvitacion() { return codigoInvitacion; }
    public Instant getInvitacionExpiraEn() { return invitacionExpiraEn; }
    public boolean isMigradoAClerk() { return migradoAClerk; }
    public boolean esUsuarioConLogin() { return tieneLogin && clerkUserId != null; }
    public boolean esInvitacionValida() {
        return codigoInvitacion != null && invitacionExpiraEn != null && invitacionExpiraEn.isAfter(Instant.now());
    }
    public void generarCodigoInvitacion() {
        this.codigoInvitacion = java.util.UUID.randomUUID().toString();
        this.invitacionExpiraEn = Instant.now().plus(java.time.Duration.ofDays(7));
    }
    /** New identity/restored identity must not reuse generations from previously signed JWTs. */
    public static long nuevaGeneracionSesion() { return SESSION_GENERATIONS.nextLong(1, Long.MAX_VALUE / 2); }
    public void setUltimoLoginExitosoEn(Instant value) { ultimoLoginExitosoEn = value; }
    public void setSuperAdmin(boolean value) { if (superAdmin != value) sessionVersion++; superAdmin = value; }
    public void setNombre(String value) { nombre = value; }
    public void setPasswordHash(String value) { sessionVersion++; passwordHash = value; }
    public void setRol(RolEmpresa value) { if (rol != value) sessionVersion++; rol = value; }
    public void setActivo(boolean value) { if (activo != value) sessionVersion++; activo = value; }
    public void setEmpresa(Empresa value) { empresa = value; }
    public void setEmail(String value) { email = value; }
    public void setClerkUserId(String value) { clerkUserId = value; }
    public void setTieneLogin(boolean value) { tieneLogin = value; }
    public void setCodigoInvitacion(String value) { codigoInvitacion = value; }
    public void setInvitacionExpiraEn(Instant value) { invitacionExpiraEn = value; }
    public void setMigradoAClerk(boolean value) { migradoAClerk = value; }
}
