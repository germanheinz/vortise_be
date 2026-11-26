package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CalendarioLaboralEmpresa {
    @Column(name = "calendario_id", unique = true)
    private Long id;
    @Column(name = "calendario_horas_lunesaviernes")
    private Double horasLunesAViernes = 8.0;
    @Column(name = "calendario_horas_sabado")
    private Double horasSabado = 4.0;
    @Column(name = "calendario_trabaja_domingo")
    private Boolean trabajaDomingo = false;
    @Column(name = "calendario_paga_doble_feriado")
    private Boolean pagaDobleFeriado = false;
    @Column(name = "calendario_paga_doble_sabado")
    private Boolean pagaDobleSabado = false;
    @Column(name = "calendario_paga_doble_no_laborable")
    private Boolean pagaDobleNoLaborable = false;
    protected CalendarioLaboralEmpresa() { }
    CalendarioLaboralEmpresa(Long id) { this.id = id; }
    public Long getId() { return id; }
    public Double getHorasLunesAViernes() { return horasLunesAViernes; }
    public Double getHorasSabado() { return horasSabado; }
    public boolean isTrabajaDomingo() { return Boolean.TRUE.equals(trabajaDomingo); }
    public boolean isPagaDobleFeriado() { return Boolean.TRUE.equals(pagaDobleFeriado); }
    public boolean isPagaDobleSabado() { return Boolean.TRUE.equals(pagaDobleSabado); }
    public boolean isPagaDobleNoLaborable() { return Boolean.TRUE.equals(pagaDobleNoLaborable); }
    public void setHorasLunesAViernes(Double value) { horasLunesAViernes = value; }
    public void setHorasSabado(Double value) { horasSabado = value; }
    public void setTrabajaDomingo(boolean value) { trabajaDomingo = value; }
    public void setPagaDobleFeriado(boolean value) { pagaDobleFeriado = value; }
    public void setPagaDobleSabado(boolean value) { pagaDobleSabado = value; }
    public void setPagaDobleNoLaborable(boolean value) { pagaDobleNoLaborable = value; }
}
