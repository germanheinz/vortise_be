package com.vortise.gestion.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Measurement of progress per item and certificate. Pure: no persistence, so every rule can be tested with plain values.
 * Periods are consecutive, so each load belongs to exactly one certificate: the first whose cutoff is on or after its date.
 */
public final class CalculoCertificado {
    private CalculoCertificado() { }

    public record Item(Long rubroId, Double cantidadTotal) { }

    public record Avance(Long rubroId, LocalDate fecha, double cantidad) { }

    /** Cutoff in chronological order; {@code completados} are the items closed with the check in that certificate. */
    public record Corte(LocalDate fechaCorte, Set<Long> completados) { }

    public record Fila(Long rubroId, double anterior, double actual, double acumulado, double ejecutado,
            double excedente, double ajuste, boolean completado, boolean marcadoEnEste) { }

    public static List<List<Fila>> calcular(List<Item> items, List<Avance> avances, List<Corte> cortes) {
        List<List<Fila>> result = new ArrayList<>();
        for (int k = 0; k < cortes.size(); k++) result.add(new ArrayList<>());
        for (Item item : items) {
            double total = item.cantidadTotal() != null && item.cantidadTotal() > 0 ? item.cantidadTotal() : Double.POSITIVE_INFINITY;
            double previoAcumulado = 0, previoEjecutado = 0;
            boolean completadoAntes = false;
            for (int k = 0; k < cortes.size(); k++) {
                Corte corte = cortes.get(k);
                double ejecutado = redondear(avances.stream()
                    .filter(a -> item.rubroId().equals(a.rubroId()) && a.fecha() != null && !a.fecha().isAfter(corte.fechaCorte()))
                    .mapToDouble(Avance::cantidad).sum());
                boolean marcadoEnEste = corte.completados().contains(item.rubroId());
                double acumulado, excedente = 0, ajuste = 0;
                if (completadoAntes) {
                    acumulado = total;
                } else if (marcadoEnEste) {
                    acumulado = total;
                    ajuste = redondear(Math.max(0, total - ejecutado));
                    excedente = excedente(ejecutado, previoEjecutado, total);
                } else {
                    acumulado = Math.min(ejecutado, total);
                    excedente = excedente(ejecutado, previoEjecutado, total);
                }
                if (Double.isInfinite(acumulado)) acumulado = ejecutado;
                acumulado = redondear(acumulado);
                result.get(k).add(new Fila(item.rubroId(), previoAcumulado, redondear(acumulado - previoAcumulado), acumulado, ejecutado,
                    excedente, ajuste, completadoAntes || marcadoEnEste, marcadoEnEste));
                previoAcumulado = acumulado;
                previoEjecutado = ejecutado;
                completadoAntes = completadoAntes || marcadoEnEste;
            }
        }
        return result;
    }

    private static double excedente(double ejecutado, double previoEjecutado, double total) {
        if (Double.isInfinite(total)) return 0;
        return redondear(Math.max(0, ejecutado - total) - Math.max(0, previoEjecutado - total));
    }

    /** Percentage of the contracted quantity; null when the item has no quantity to measure against. */
    public static Double porcentaje(double cantidad, Double total) {
        return total == null || !(total > 0) ? null : redondear(cantidad / total * 100);
    }

    static double redondear(double value) { return Math.round(value * 10_000.0) / 10_000.0; }
}
