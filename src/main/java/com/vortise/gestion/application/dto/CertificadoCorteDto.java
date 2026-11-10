package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CertificadoCorteDto(@NotNull LocalDate fechaCorte) { }
