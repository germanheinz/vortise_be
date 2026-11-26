package com.vortise.gestion.domain.rule;

/** Fórmulas de PREV VS REAL.xlsx. El redondeo se aplica al presentar los resultados. */
public final class PrevistoRealCalculator {
    private static final double PRODUCCION_LENTA_HASTA = -0.5;
    private static final double PRODUCCION_RAPIDA_DESDE = 1.0;
    private static final double PRODUCTIVIDAD_ESTABLE_DESDE = 1.0;
    private static final double PRODUCTIVIDAD_ESTABLE_HASTA = 1.2;
    private static final double RECURSO_SUFICIENTE_DESDE = -1.0;
    private static final double RECURSO_SUFICIENTE_HASTA = 1.0;

    private PrevistoRealCalculator() {
    }

    public record Metrics(
            Double produccion,
            String estadoProduccion,
            Double productividad,
            String estadoProductividad,
            Double recurso,
            String estadoRecurso,
            Double desvio,
            Double proyeccion,
            Double cantidadEquivalente,
            Double rendimientoPrevisto,
            Double rendimientoReal) {
    }

    public static Metrics calculate(
            Double cantidadPrevista,
            Double horasOficialesPrevistas,
            Double horasAyudantesPrevistas,
            Double cantidadReal,
            Double horasOficialesReales,
            Double horasAyudantesReales,
            Double costoHoraOficial,
            Double costoHoraAyudante,
            Double horasJornada) {
        Double rendimientoPrevisto = esPositivo(cantidadPrevista)
                && esNoNegativo(horasOficialesPrevistas) && esNoNegativo(horasAyudantesPrevistas)
            ? finito((horasOficialesPrevistas + horasAyudantesPrevistas) / cantidadPrevista)
            : null;
        Double horasReales = esNoNegativo(horasOficialesReales) && esNoNegativo(horasAyudantesReales)
            ? finito(horasOficialesReales + horasAyudantesReales)
            : null;
        Double rendimientoReal = esPositivo(cantidadReal) && horasReales != null
            ? finito(horasReales / cantidadReal)
            : null;

        // La cantidad equivalente expresa las horas reales en unidades de obra al rendimiento previsto.
        Double cantidadEquivalente = esPositivo(rendimientoPrevisto) && horasReales != null
            ? finito(horasReales / rendimientoPrevisto)
            : null;
        Double produccion = esNoNegativo(cantidadReal) && cantidadEquivalente != null
            ? finito(cantidadReal - cantidadEquivalente)
            : null;
        Double productividad = esPositivo(rendimientoPrevisto) && esPositivo(rendimientoReal)
            ? finito(rendimientoPrevisto / rendimientoReal)
            : null;
        Double recurso = rendimientoPrevisto != null && esNoNegativo(cantidadReal)
                && horasReales != null && esPositivo(horasJornada)
            ? finito((cantidadReal * rendimientoPrevisto - horasReales) / horasJornada)
            : null;

        Double desvio = null;
        if (rendimientoPrevisto != null && esNoNegativo(cantidadReal) && horasReales != null
                && esNoNegativo(costoHoraOficial) && esNoNegativo(costoHoraAyudante)) {
            double costoUnitarioPrevisto = horasOficialesPrevistas / cantidadPrevista * costoHoraOficial
                + horasAyudantesPrevistas / cantidadPrevista * costoHoraAyudante;
            desvio = finito(cantidadReal * costoUnitarioPrevisto
                - horasOficialesReales * costoHoraOficial - horasAyudantesReales * costoHoraAyudante);
        }
        Double proyeccion = desvio != null && cantidadReal > 0
            ? finito(desvio / cantidadReal * Math.max(0, cantidadPrevista - cantidadReal))
            : null;

        return new Metrics(
            produccion, estadoProduccion(produccion),
            productividad, estadoProductividad(productividad),
            recurso, estadoRecurso(recurso),
            desvio, proyeccion, cantidadEquivalente, rendimientoPrevisto, rendimientoReal);
    }

    private static String estadoProduccion(Double produccion) {
        if (produccion == null) return null;
        if (produccion < PRODUCCION_LENTA_HASTA) return "LENTA";
        return produccion < PRODUCCION_RAPIDA_DESDE ? "ESTABLE" : "RAPIDA";
    }

    private static String estadoProductividad(Double productividad) {
        if (productividad == null) return null;
        if (productividad < PRODUCTIVIDAD_ESTABLE_DESDE) return "BAJA";
        return productividad <= PRODUCTIVIDAD_ESTABLE_HASTA ? "ESTABLE" : "ALTA";
    }

    private static String estadoRecurso(Double recurso) {
        if (recurso == null) return null;
        if (recurso < RECURSO_SUFICIENTE_DESDE) return "INSUFICIENTE";
        return recurso <= RECURSO_SUFICIENTE_HASTA ? "SUFICIENTE" : "EXCEDIDO";
    }

    private static boolean esPositivo(Double valor) {
        return esNoNegativo(valor) && valor > 0;
    }

    private static boolean esNoNegativo(Double valor) {
        return valor != null && Double.isFinite(valor) && valor >= 0;
    }

    private static Double finito(double valor) {
        return Double.isFinite(valor) ? valor : null;
    }
}
