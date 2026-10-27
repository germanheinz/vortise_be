package com.vortise.gestion.application.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class SuperAdminDtos {
    private SuperAdminDtos() { }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }
    public record EstadoCantidadDto(String estado, long cantidad) { }
    public record PagoResumenDto(String estado, String moneda, long cantidad, long montoCentavos, long montoDevueltoCentavos, long montoNetoCentavos) {
        public PagoResumenDto(String estado, String moneda, long cantidad, long montoCentavos) {
            this(estado, moneda, cantidad, montoCentavos, 0, "PAGADO".equals(estado) ? montoCentavos : 0);
        }
    }
    public record DashboardDto(Instant calculadoEn, Instant datosActualizadosEn, long empresas, long usuariosActivos,
            long usuariosInactivos, List<EstadoCantidadDto> suscripcionesPorEstado,
            List<PagoResumenDto> pagosRecientesPorEstadoYMoneda, long pagosFallidos,
            long suscripcionesPastDue, long suscripcionesInactivas, long pruebasPorVencer, int ventanaPruebasDias,
            String fuentePagos) { }
    public record EmpresaDto(Long id, String nombre, LocalDateTime creadoEn, long usuariosActivos,
            String planCodigo, String estadoSuscripcion, String creadorNombre, String creadorEmail,
            long usuariosTotales, long obras, long restricciones, long registrosHoras) { }
    public record UsuarioDto(Long id, Long empresaId, String empresaNombre, String nombre, String email,
            String rol, boolean activo, Instant ultimoLoginExitosoEn,
            FacturacionUsuarioDto suscripcionUsuario, FacturacionUsuarioDto suscripcionEmpresa) { }
    public record FacturacionUsuarioDto(String planCodigo, String estado, Instant inicio, Instant finPrueba,
            Instant finPeriodoActual, boolean cancelaAlFinal) { }
    public record SuscripcionDto(Long id, String titularTipo, String titularNombre, String titularEmail,
            String empresaNombre, String planCodigo, String planNombre, String estado, Instant finPrueba,
            Instant finPeriodoActual, boolean cancelaAlFinal, Instant actualizadoEn, Instant inicio) { }
    public record PagoDto(Long id, String empresaNombre, String titularNombre, String titularEmail,
            String planCodigo, String estado, long montoCentavos, String moneda, Instant fecha, long montoDevueltoCentavos, long montoNetoCentavos, String estadoDevolucion, Instant fechaDevolucion,
            Long intentos, Instant proximoIntento, String estadoFactura, Instant fechaPago,
            Instant periodoInicio, Instant periodoFin, Long suscripcionId) { }
}
