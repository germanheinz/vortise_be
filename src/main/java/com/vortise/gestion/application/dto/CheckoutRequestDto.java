package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.TitularSuscripcion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequestDto(@NotBlank String planCodigo, @NotNull TitularSuscripcion titular) { }
