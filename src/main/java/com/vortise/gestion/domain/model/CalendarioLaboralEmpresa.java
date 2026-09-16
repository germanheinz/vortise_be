package com.vortise.gestion.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "calendarios_laborales_empresa")
public class CalendarioLaboralEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;
    private Double horasLunesAViernes = 8.0;
    private Double horasSabado = 4.0;
    private boolean trabajaDomingo;
    private boolean pagaDobleFeriado;
    private boolean pagaDobleSabado;
    private boolean pagaDobleNoLaborable;
    protected CalendarioLaboralEmpresa() { }
    public CalendarioLaboralEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Double getHorasLunesAViernes() { return horasLunesAViernes; }
    public Double getHorasSabado() { return horasSabado; }
    public boolean isTrabajaDomingo() { return trabajaDomingo; }
    public boolean isPagaDobleFeriado() { return pagaDobleFeriado; }
    public boolean isPagaDobleSabado() { return pagaDobleSabado; }
    public boolean isPagaDobleNoLaborable() { return pagaDobleNoLaborable; }
    public void setHorasLunesAViernes(Double value) { horasLunesAViernes = value; }
    public void setHorasSabado(Double value) { horasSabado = value; }
    public void setTrabajaDomingo(boolean value) { trabajaDomingo = value; }
    public void setPagaDobleFeriado(boolean value) { pagaDobleFeriado = value; }
    public void setPagaDobleSabado(boolean value) { pagaDobleSabado = value; }
    public void setPagaDobleNoLaborable(boolean value) { pagaDobleNoLaborable = value; }
}
