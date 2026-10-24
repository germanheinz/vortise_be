package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.SuperAdminDtos.DashboardDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.EmpresaDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.EstadoCantidadDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.PagoDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.PagoResumenDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.PageDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.SuscripcionDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.UsuarioDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.PagoSuscripcion;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaConteoProjection;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.SuperAdminCreadorProjection;
import com.vortise.gestion.domain.repository.SuperAdminSuscripcionEmpresaProjection;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SuperAdminConsultaUseCase {
    private static final String PLATFORM_TAX_ID = "PLATFORM-0001";
    private static final int VENTANA_PRUEBAS_DIAS = 7;
    private static final int VENTANA_PAGOS_DIAS = 30;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final ProyectoRepository proyectos;
    private final RestriccionObraRepository restricciones;
    private final RegistroHorasRepository registrosHoras;
    private final SuscripcionRepository suscripciones;
    private final PagoSuscripcionRepository pagos;

    public SuperAdminConsultaUseCase(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios,
            ProyectoRepository proyectos, RestriccionObraRepository restricciones, RegistroHorasRepository registrosHoras,
            SuscripcionRepository suscripciones, PagoSuscripcionRepository pagos) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.proyectos = proyectos;
        this.restricciones = restricciones;
        this.registrosHoras = registrosHoras;
        this.suscripciones = suscripciones;
        this.pagos = pagos;
    }

    public DashboardDto dashboard() {
        Instant ahora = Instant.now();
        Long empresaInternaId = empresaInternaId();
        Instant ultimaSuscripcion = suscripciones.ultimaActualizacionSuperAdmin(empresaInternaId);
        Instant ultimoPago = pagos.ultimaActualizacionSuperAdmin(empresaInternaId);
        Instant actualizado = java.util.stream.Stream.of(ultimaSuscripcion, ultimoPago).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        List<EstadoCantidadDto> estados = suscripciones.contarEstadosSuperAdmin(empresaInternaId).stream()
            .map(row -> new EstadoCantidadDto((String) row[0], ((Number) row[1]).longValue()))
            .sorted(Comparator.comparing((EstadoCantidadDto row) -> row.estado(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        List<PagoResumenDto> pagosRecientes = pagos.resumirRecientesSuperAdmin(empresaInternaId,
                ahora.minusSeconds(VENTANA_PAGOS_DIAS * 24L * 60 * 60)).stream()
            .map(row -> new PagoResumenDto((String) row[0], (String) row[1], ((Number) row[2]).longValue(), ((Number) row[3]).longValue()))
            .sorted(Comparator.comparing((PagoResumenDto row) -> row.estado(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(row -> row.moneda(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        return new DashboardDto(ahora, actualizado, Math.max(0, empresas.count() - (empresaInternaId == null ? 0 : 1)),
            usuarios.contarActivosSuperAdmin(empresaInternaId), usuarios.contarInactivosSuperAdmin(empresaInternaId), estados,
            pagosRecientes, pagos.contarFallidosSuperAdmin(empresaInternaId),
            suscripciones.contarEstadoSuperAdmin(empresaInternaId, "past_due"),
            suscripciones.contarInactivasSuperAdmin(empresaInternaId),
            suscripciones.contarPruebasPorVencer(empresaInternaId, ahora, ahora.plusSeconds(VENTANA_PRUEBAS_DIAS * 24L * 60 * 60)),
            VENTANA_PRUEBAS_DIAS, "Pagos locales sincronizados por webhook; resumen de pagos de los últimos 30 días. No es tiempo real.");
    }

    @SuppressWarnings("null")
    public PageDto<EmpresaDto> empresas(int page, int size, String q) {
        Long empresaInternaId = empresaInternaId();
        Page<Empresa> result = empresas.buscarSuperAdmin(empresaInternaId, normalizarBusqueda(q), pageable(page, size));
        List<Empresa> content = result.getContent();
        if (content.isEmpty()) return new PageDto<>(List.of(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());

        List<Long> empresaIds = content.stream().map(Empresa::getId).filter(Objects::nonNull).toList();
        List<Long> creadorIds = content.stream().map(Empresa::getCreadoPorUsuarioId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> usuariosTotales = counts(usuarios.contarUsuariosPorEmpresa(empresaIds));
        Map<Long, Long> usuariosActivos = counts(usuarios.contarUsuariosActivosPorEmpresa(empresaIds));
        Map<Long, Long> obras = counts(proyectos.contarPorEmpresaRelacionada(empresaIds));
        Map<Long, Long> restriccionesPorEmpresa = counts(restricciones.contarPorEmpresaRelacionada(empresaIds));
        Map<Long, Long> horas = counts(registrosHoras.contarPorEmpresaRelacionada(empresaIds));
        Map<Long, SuperAdminCreadorProjection> creadores = creadorIds.isEmpty() ? Map.of()
            : usuarios.buscarCreadoresSuperAdmin(creadorIds).stream().collect(Collectors.toMap(
                SuperAdminCreadorProjection::getUsuarioId, Function.identity()));
        Map<Long, SuperAdminSuscripcionEmpresaProjection> suscripcionesPorEmpresa = suscripciones
            .buscarResumenEmpresaSuperAdmin(empresaIds).stream().collect(Collectors.toMap(
                SuperAdminSuscripcionEmpresaProjection::getEmpresaId, Function.identity()));
        return mapPage(result, empresa -> {
            Long creadorId = empresa.getCreadoPorUsuarioId();
            SuperAdminCreadorProjection creador = creadorId == null ? null : creadores.get(creadorId);
            SuperAdminSuscripcionEmpresaProjection suscripcion = suscripcionesPorEmpresa.get(empresa.getId());
            return new EmpresaDto(empresa.getId(), empresa.getNombre(), empresa.getCreadoEn(),
                usuariosActivos.getOrDefault(empresa.getId(), 0L),
                suscripcion == null ? null : suscripcion.getPlanCodigo(), suscripcion == null ? null : suscripcion.getEstado(),
                creador == null ? null : creador.getNombre(), creador == null ? null : creador.getEmail(),
                usuariosTotales.getOrDefault(empresa.getId(), 0L), obras.getOrDefault(empresa.getId(), 0L),
                restriccionesPorEmpresa.getOrDefault(empresa.getId(), 0L), horas.getOrDefault(empresa.getId(), 0L));
        });
    }

    public PageDto<UsuarioDto> usuarios(int page, int size, String q, Long empresaId, Boolean activo) {
        return mapPage(usuarios.buscarSuperAdmin(empresaInternaId(), empresaId, activo, normalizarBusqueda(q), pageable(page, size)), usuario ->
            new UsuarioDto(usuario.getId(), usuario.getEmpresa().getId(), usuario.getEmpresa().getNombre(),
                usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(), usuario.isActivo(), usuario.getUltimoLoginExitosoEn()));
    }

    public PageDto<SuscripcionDto> suscripciones(int page, int size, String estado, Long empresaId) {
        return mapPage(suscripciones.buscarSuperAdmin(empresaInternaId(), normalizar(estado), empresaId, pageable(page, size)),
            this::toSuscripcionDto);
    }

    public PageDto<PagoDto> pagos(int page, int size, String estado, LocalDate desde, LocalDate hasta) {
        Instant desdeInstante = desde == null ? null : desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant hastaInstante = hasta == null ? null : hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return mapPage(pagos.buscarSuperAdmin(empresaInternaId(), normalizar(estado), desdeInstante, hastaInstante,
            pageable(page, size)), this::toPagoDto);
    }

    private SuscripcionDto toSuscripcionDto(Suscripcion suscripcion) {
        UsuarioEmpresa usuario = suscripcion.getUsuario();
        Empresa empresa = suscripcion.getEmpresa() != null ? suscripcion.getEmpresa() : usuario == null ? null : usuario.getEmpresa();
        return new SuscripcionDto(suscripcion.getId(), suscripcion.getTitular().name(),
            usuario != null ? usuario.getNombre() : empresa == null ? "Sin titular" : empresa.getNombre(),
            usuario == null ? null : usuario.getEmail(), empresa == null ? null : empresa.getNombre(),
            suscripcion.getPlan() == null ? null : suscripcion.getPlan().getCodigo(),
            suscripcion.getPlan() == null ? null : suscripcion.getPlan().getNombre(), suscripcion.getEstado(),
            suscripcion.getFinPrueba(), suscripcion.getFinPeriodoActual(), suscripcion.isCancelaAlFinal(),
            suscripcion.getActualizadoEn());
    }

    private PagoDto toPagoDto(PagoSuscripcion pago) {
        Suscripcion suscripcion = pago.getSuscripcion();
        UsuarioEmpresa usuario = suscripcion.getUsuario();
        Empresa empresa = suscripcion.getEmpresa() != null ? suscripcion.getEmpresa() : usuario == null ? null : usuario.getEmpresa();
        return new PagoDto(pago.getId(), empresa == null ? null : empresa.getNombre(),
            usuario != null ? usuario.getNombre() : empresa == null ? "Sin titular" : empresa.getNombre(),
            usuario == null ? null : usuario.getEmail(), suscripcion.getPlan() == null ? null : suscripcion.getPlan().getCodigo(),
            pago.getEstado(), pago.getMontoCentavos(), pago.getMoneda(), pago.getFecha());
    }

    private Long empresaInternaId() {
        return empresas.findByIdentificacionFiscal(PLATFORM_TAX_ID).map(empresa -> empresa.getId()).orElse(null);
    }

    @SuppressWarnings("null")
    private static Map<Long, Long> counts(Collection<EmpresaConteoProjection> rows) {
        return rows.stream().collect(Collectors.toMap(EmpresaConteoProjection::getEmpresaId, EmpresaConteoProjection::getCantidad));
    }

    private static String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizarBusqueda(String value) {
        return value == null ? "" : value.trim();
    }

    private static PageRequest pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"));
    }

    private static <S, T> PageDto<T> mapPage(Page<S> page, java.util.function.Function<S, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }
}
