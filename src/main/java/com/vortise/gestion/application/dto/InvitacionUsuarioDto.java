package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.RolEmpresa;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class InvitacionUsuarioDto {
    private InvitacionUsuarioDto() { }
    /** {@code obraIds} only applies to JEFE_DE_OBRA. */
    public record Crear(@NotBlank @Email @Size(max = 254) String email, @NotNull RolEmpresa rol,
                        @Size(max = 200) List<@NotNull @Positive Long> obraIds) { }
    /** The link carries the only copy of the token: it cannot be recovered later. */
    public record Creada(Long id, String enlace, Instant venceEn, String email, RolEmpresa rol, int obras) {
        @Override public String toString() { return "UserInvitationResult[redacted]"; }
    }
    public record Resumen(Long id, String email, RolEmpresa rol, int obras, String estado, Instant venceEn) { }
    public record Token(@NotBlank @Size(max = 128) String token) { @Override public String toString() { return "UserInvitationToken[redacted]"; } }
    public record Consulta(String empresaNombre, String email, RolEmpresa rol, Instant venceEn) { }
    public record Aceptar(@NotBlank @Size(max = 128) String token, @NotBlank @Size(max = 160) String nombre,
                          @NotBlank @Size(min = 8, max = 72) String password) {
        @Override public String toString() { return "UserInvitationAcceptance[redacted]"; }
    }
}
