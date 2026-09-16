package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(unique = true)
    private String identificacionFiscal;

    @Column(nullable = false)
    private int limiteUsuarios = 5;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Empresa() {
    }

    public Empresa(String nombre, String identificacionFiscal) {
        this.nombre = nombre;
        this.identificacionFiscal = identificacionFiscal;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIdentificacionFiscal() {
        return identificacionFiscal;
    }

    public int getLimiteUsuarios() {
        return limiteUsuarios;
    }

    public void setLimiteUsuarios(int limiteUsuarios) {
        this.limiteUsuarios = limiteUsuarios;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setIdentificacionFiscal(String identificacionFiscal) {
        this.identificacionFiscal = identificacionFiscal;
    }
}
