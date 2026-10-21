package com.vortise.gestion.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlanSuscripcionDto(String codigo, String nombre, BigDecimal precioMensual, String moneda, int diasPrueba,
                                 List<String> beneficios, boolean disponible) { }
