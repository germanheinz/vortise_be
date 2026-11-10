package com.vortise.gestion.application.dto;

import java.time.LocalDate;

public record CertificadoResumenDto(
    Long id, Integer numero, String etiqueta, String periodicidad, LocalDate periodoDesde, LocalDate fechaCorte, String estado
) { }
