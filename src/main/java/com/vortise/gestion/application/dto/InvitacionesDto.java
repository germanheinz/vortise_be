package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class InvitacionesDto {
    private InvitacionesDto() { }
    public record Crear(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> empresaIds) { }
    public record Token(@NotBlank @Size(max = 128) String token) { @Override public String toString() { return "InvitationToken[redacted]"; } }
    public record Aceptar(@NotBlank @Size(max = 128) String token,
        @NotBlank @Size(max = 160) String nombre, @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password) { @Override public String toString() { return "InvitationAcceptance[redacted]"; } }
    public record EmpresaAcceso(Long id, String nombre) { }
    public record Creada(Long id, String enlace, Instant venceEn, List<EmpresaAcceso> empresas) { @Override public String toString() { return "InvitationResult[redacted]"; } }
    public record Resumen(Long id, Instant venceEn, String estado, List<EmpresaAcceso> empresas) { }
    public record Equipo(List<Resumen> invitaciones, long invitadosActivos, int limite, boolean puedeInvitar,
        List<EmpresaAcceso> empresasDisponibles) { }
    public record Consulta(List<EmpresaAcceso> empresas, Instant venceEn) { }
}
