package com.vortise.gestion.application.helper;

import com.vortise.gestion.application.dto.RestriccionDto;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RestriccionResumenGerencial {
    public enum Filtro { TODAS, ABIERTAS, VENCIDAS }
    private enum Estado { ABIERTA, VENCIDA, CERRADA }
    private static final Set<String> RESUELTAS = Set.of("RESUELTO", "RESUELTA", "LIBERADO");
    private static final int LIMITE_LISTADO = 10;

    private RestriccionResumenGerencial() {
    }

    public static Respuesta calcular(Long proyectoId, String obra, LocalDate fecha, Filtro filtro,
            String prioridad, List<RestriccionDto> restricciones) {
        String prioridadFiltro = normalizar(prioridad);
        var filas = restricciones.stream()
            .filter(dto -> dto.fechaSolicitud() == null || !dto.fechaSolicitud().isAfter(fecha))
            .map(dto -> new Fila(dto, estado(dto, fecha)))
            .toList();

        long abiertas = filas.stream().filter(Fila::abierta).count();
        long vencidas = filas.stream().filter(fila -> fila.estado() == Estado.VENCIDA).count();
        long altaPrioridad = filas.stream().filter(Fila::abierta)
            .filter(fila -> normalizar(fila.dto().prioridad()).equals("ALTA")).count();

        var coincidencias = filas.stream()
            .filter(fila -> prioridadFiltro.isEmpty() || normalizar(fila.dto().prioridad()).equals(prioridadFiltro))
            .filter(fila -> switch (filtro) {
                case TODAS -> true;
                case ABIERTAS -> fila.abierta();
                case VENCIDAS -> fila.estado() == Estado.VENCIDA;
            })
            .sorted(Comparator.comparing((Fila fila) -> fila.dto().fechaMaximaEntrega(),
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(fila -> fila.dto().id()))
            .toList();

        var detalle = coincidencias.stream().limit(LIMITE_LISTADO)
            .map(Fila::detalle).toList();
        return new Respuesta(proyectoId, obra, fecha, coincidencias.size(), abiertas, vencidas, altaPrioridad,
            "/proyectos/" + proyectoId + "/restricciones", detalle);
    }

    private static Estado estado(RestriccionDto dto, LocalDate corte) {
        LocalDate solicitud = dto.fechaSolicitud();
        LocalDate limite = dto.fechaMaximaEntrega();
        LocalDate entrega = dto.fechaRealEntrega();
        boolean resuelta = RESUELTAS.contains(normalizar(dto.estado()));
        if (solicitud == null || limite == null || limite.isBefore(solicitud)
                || (entrega != null && entrega.isBefore(solicitud))) {
            return entrega == null && !resuelta ? Estado.ABIERTA : Estado.CERRADA;
        }
        if ((entrega == null && resuelta) || (entrega != null && !entrega.isAfter(corte))) {
            return Estado.CERRADA;
        }
        return limite.isBefore(corte) ? Estado.VENCIDA : Estado.ABIERTA;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    private record Fila(RestriccionDto dto, Estado estado) {
        boolean abierta() {
            return estado != Estado.CERRADA;
        }

        Detalle detalle() {
            return new Detalle(dto.id(), dto.restriccion(), dto.estado(), dto.prioridad(),
                dto.fechaMaximaEntrega(), estado == Estado.VENCIDA);
        }
    }

    public record Detalle(Long id, String descripcion, String estado, String prioridad,
                          LocalDate fechaMaximaEntrega, boolean vencida) { }

    public record Respuesta(Long proyectoId, String obra, LocalDate corte, int total,
                            long abiertas, long vencidas, long altaPrioridad, String enlace, List<Detalle> detalle) { }
}
