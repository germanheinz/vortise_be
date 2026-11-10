package com.vortise.gestion.application.dto;

import java.time.LocalDate;

/** {@code tipo} is EXCEDENTE (loaded above the contract) or AJUSTE (certified by closing an item below 100%). */
public record CertificadoExcedenteDto(
    Long certificadoId, String certificado, String empresa, String tipo, String item, String unidad,
    Double cantidadContratada, double cantidadEjecutada, double cantidad, Double porcentaje, LocalDate fecha, String usuario
) { }
