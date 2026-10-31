package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CategoriaManoObraCostoDto(@NotNull Long id, @DecimalMin("0.0") Double costoHora) { }