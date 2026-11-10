package com.vortise.gestion.application.dto;

/** One measured item; percentages are null when the item has no contracted quantity. */
public record CertificadoFilaDto(
    Long rubroId, String codigo, String grupoCodigo, String grupoNombre, String nombre, String unidad, Double cantidadTotal,
    double anteriorCant, Double anteriorPct, double actualCant, Double actualPct, double acumuladoCant, Double acumuladoPct,
    double excedenteCant, Double excedentePct, double ajusteCant, Double ajustePct,
    boolean completado, boolean puedeAlternarCompletado
) { }
