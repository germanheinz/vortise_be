package com.vortise.gestion.application.dto;

import com.vortise.gestion.model.RolEmpresa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioEmpresaDto(Long id, @Email @NotBlank String email, @NotBlank String nombre, @NotNull RolEmpresa rol, boolean activo, String password,
                                SuscripcionResumenDto suscripcion, java.util.List<Long> obrasAsignadas) {
    public UsuarioEmpresaDto(Long id, String email, String nombre, RolEmpresa rol, boolean activo, String password, SuscripcionResumenDto suscripcion) {
        this(id, email, nombre, rol, activo, password, suscripcion, java.util.List.of());
    }
}
