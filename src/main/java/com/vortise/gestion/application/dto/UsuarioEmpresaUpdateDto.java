package com.vortise.gestion.application.dto;

import com.vortise.gestion.model.RolEmpresa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Editing does not rename the login email or accept a replacement identity.
 * {@code obrasAsignadas} are the sites a JEFE_DE_OBRA may write to; {@code null} leaves them unchanged.
 */
public record UsuarioEmpresaUpdateDto(@NotBlank String nombre, @NotNull RolEmpresa rol, boolean activo, String password,
                                      java.util.List<Long> obrasAsignadas) {
    public UsuarioEmpresaUpdateDto(String nombre, RolEmpresa rol, boolean activo, String password) {
        this(nombre, rol, activo, password, null);
    }
}
