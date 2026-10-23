package com.vortise.gestion.application.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class SuperAdminDtos {
    private SuperAdminDtos() { }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }
    public record EstadoCantidadDto(String estado, long cantidad) { }
    public record PagoResumenDto(String estado, String moneda, long cantidad, long montoCentavos) { }
    public record DashboardDto(Instant calculadoEn, Instant datosActualizadosEn, long empresas, long usuariosActivos,
            long usuariosInactivos, List<EstadoCantidadDto> suscripcionesPorEstado,
            List<PagoResumenDto> pagosRecientesPorEstadoYMoneda, long pagosFallidos,
            long suscripcionesPastDue, long suscripcionesInactivas, long pruebasPorVencer, int ventanaPruebasDias,
            String fuentePagos) { }
    public record EmpresaDto(Long id, String nombre, LocalDateTime creadoEn, long usuariosActivos,
            String planCodigo, String estadoSuscripcion) { }
    public record UsuarioDto(Long id, Long empresaId, String empresaNombre, String nombre, String email,
            String rol, boolean activo) { }
    public record SuscripcionDto(Long id, String titularTipo, String titularNombre, String titularEmail,
            String empresaNombre, String planCodigo, String planNombre, String estado, Instant finPrueba,
            Instant finPeriodoActual, boolean cancelaAlFinal, Instant actualizadoEn) { }
    public record PagoDto(Long id, String empresaNombre, String titularNombre, String titularEmail,
            String planCodigo, String estado, long montoCentavos, String moneda, Instant fecha) { }
}
