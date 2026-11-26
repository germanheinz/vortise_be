package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Company registration for a verified external identity; the email comes from the identity, not the body. */
public record RegistroConIdentidadDto(@NotBlank @Size(max = 120) String empresaNombre, @NotBlank @Size(max = 120) String nombre) { }
