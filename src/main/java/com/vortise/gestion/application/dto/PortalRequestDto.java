package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.TitularSuscripcion;
import jakarta.validation.constraints.NotNull;

public record PortalRequestDto(@NotNull TitularSuscripcion titular) { }
