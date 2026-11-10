package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** {@code subcontratistaId} null selects the own labor; {@code numeroInicial} only applies to the first monthly certificate. */
public record CertificadoCrearDto(Long subcontratistaId, @NotBlank String periodicidad, @NotNull LocalDate fechaCorte, Integer numeroInicial) { }
