package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Password max 72: BCrypt ignores bytes beyond that. */
public record RegistroRequestDto(@NotBlank @Size(max = 120) String empresaNombre, @NotBlank @Size(max = 120) String nombre,
                                 @Email @NotBlank @Size(max = 254) String email, @NotBlank @Size(min = 8, max = 72) String password) { }
