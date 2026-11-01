package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResponsableEmpresaDto(
    Long id,
    @NotBlank String rol,
    @NotBlank String nombre,
    String email,
    String telefono,
    @Size(max = 120) String area
) {
    public ResponsableEmpresaDto(Long id, String rol, String nombre, String email, String telefono) {
        this(id, rol, nombre, email, telefono, null);
    }
}
