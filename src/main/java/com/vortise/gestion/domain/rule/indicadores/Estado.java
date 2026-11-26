package com.vortise.gestion.domain.rule.indicadores;

/** Band of an index: below 0,9, from 0,9 to 1,1 (both included) or above 1,1. */
public enum Estado {
    BAJO, ESTABLE, ALTO;

    public static final double ESTABLE_DESDE = 0.9;
    public static final double ESTABLE_HASTA = 1.1;

    public static Estado de(Double indice) {
        if (indice == null) return null;
        double visible = Redondeo.dosDecimales(indice);
        if (visible < ESTABLE_DESDE) return BAJO;
        return visible <= ESTABLE_HASTA ? ESTABLE : ALTO;
    }

    static String produccion(Double indice) { return etiqueta(de(indice), "LENTA", "ESTABLE", "RAPIDA"); }
    static String productividad(Double indice) { return etiqueta(de(indice), "BAJA", "ESTABLE", "ALTA"); }
    static String recursos(Double indice) { return etiqueta(de(indice), "INSUFICIENTE", "SUFICIENTE", "EXCEDIDO"); }

    private static String etiqueta(Estado estado, String bajo, String estable, String alto) {
        if (estado == null) return null;
        return switch (estado) {
            case BAJO -> bajo;
            case ESTABLE -> estable;
            case ALTO -> alto;
        };
    }
}
