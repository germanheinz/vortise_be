package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.RolEmpresa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Editing does not rename the login email or accept a replacement identity. */
public record UsuarioEmpresaUpdateDto(@NotBlank String nombre, @NotNull RolEmpresa rol, boolean activo, String password) { }
