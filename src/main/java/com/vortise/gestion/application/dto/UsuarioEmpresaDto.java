package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.RolEmpresa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioEmpresaDto(Long id, @Email @NotBlank String email, @NotBlank String nombre, @NotNull RolEmpresa rol, boolean activo, String password) { }
