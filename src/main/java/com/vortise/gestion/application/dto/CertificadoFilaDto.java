package com.vortise.gestion.application.dto;

/** One measured item; percentages are null when the item has no contracted quantity. */
public record CertificadoFilaDto(
    Long rubroId, String codigo, String grupoCodigo, String grupoNombre, String nombre, String unidad, Double cantidadTotal,
    double anteriorCant, Double anteriorPct, double actualCant, Double actualPct, double acumuladoCant, Double acumuladoPct,
    double excedenteCant, Double excedentePct, double ajusteCant, Double ajustePct,
    boolean completado, boolean puedeAlternarCompletado,
    /** Contract of the rubro the item belongs to; null when it has none. */
    Long contratoId, String contratoNombre,
    /** Intermediate nodes between the rubro and the item, outermost first; empty in 2-layer structures. */
    java.util.List<CertificadoNodoDto> intermedios
) { }
