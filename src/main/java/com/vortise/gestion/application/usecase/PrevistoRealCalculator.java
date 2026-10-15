package com.vortise.gestion.application.usecase;

/** Pure calculations from PREV VS REAL.xlsx. Full precision until presentation. */
public final class PrevistoRealCalculator {
    private PrevistoRealCalculator() { }

    public record Metrics(Double produccion, String estadoProduccion, Double productividad,
            String estadoProductividad, Double recurso, String estadoRecurso,
            Double desvio, Double proyeccion, Double cantidadEquivalente,
            Double rendimientoPrevisto, Double rendimientoReal) { }

    private static boolean valid(Double n) { return n != null && Double.isFinite(n) && n >= 0; }
    private static Double finite(double n) { return Double.isFinite(n) ? n : null; }

    public static Metrics calculate(Double plannedQuantity, Double plannedOf, Double plannedAy,
            Double actualQuantity, Double actualOf, Double actualAy,
            Double costOf, Double costAy, Double dailyHours) {
        Double planned = valid(plannedQuantity) && plannedQuantity > 0 && valid(plannedOf) && valid(plannedAy)
            ? finite((plannedOf + plannedAy) / plannedQuantity) : null;
        Double hours = valid(actualOf) && valid(actualAy) ? finite(actualOf + actualAy) : null;
        Double real = valid(actualQuantity) && actualQuantity > 0 && hours != null ? finite(hours / actualQuantity) : null;
        Double equivalent = planned != null && planned > 0 && hours != null ? finite(hours / planned) : null;
        Double production = valid(actualQuantity) && equivalent != null ? finite(actualQuantity - equivalent) : null;
        Double productivity = planned != null && planned > 0 && real != null && real > 0 ? finite(planned / real) : null;
        Double resource = planned != null && valid(actualQuantity) && hours != null && valid(dailyHours) && dailyHours > 0
            ? finite((actualQuantity * planned - hours) / dailyHours) : null;
        Double variance = planned != null && valid(actualQuantity) && hours != null && valid(costOf) && valid(costAy)
            ? finite(actualQuantity * (plannedOf / plannedQuantity * costOf + plannedAy / plannedQuantity * costAy)
                - actualOf * costOf - actualAy * costAy) : null;
        Double projection = variance != null && actualQuantity > 0
            ? finite(variance / actualQuantity * Math.max(0, plannedQuantity - actualQuantity)) : null;
        return new Metrics(production, production == null ? null : production < -0.5 ? "LENTA" : production < 1 ? "ESTABLE" : "RAPIDA",
            productivity, productivity == null ? null : productivity < 1 ? "BAJA" : productivity <= 1.2 ? "ESTABLE" : "ALTA",
            resource, resource == null ? null : resource < -1 ? "INSUFICIENTE" : resource <= 1 ? "SUFICIENTE" : "EXCEDIDO",
            variance, projection, equivalent, planned, real);
    }
}
