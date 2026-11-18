package com.vortise.gestion.application.dto;

public record LoginResponseDto(String token, Long usuarioId, Long empresaId, String empresaNombre, String nombre, String email, String rol,
                               boolean superAdmin, boolean suscripcionActiva, String planCodigo, boolean tieneFoto,
                               java.util.List<Long> obras) {
    public LoginResponseDto(String token, Long usuarioId, Long empresaId, String empresaNombre, String nombre, String email, String rol,
                            boolean superAdmin, boolean suscripcionActiva, String planCodigo, boolean tieneFoto) {
        this(token, usuarioId, empresaId, empresaNombre, nombre, email, rol, superAdmin, suscripcionActiva, planCodigo, tieneFoto, java.util.List.of());
    }
}
