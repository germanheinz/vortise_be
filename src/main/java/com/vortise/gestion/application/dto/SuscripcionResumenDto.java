package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.model.TitularSuscripcion;
import java.math.BigDecimal;
import java.time.Instant;

/** Read-only subscription status; never includes Stripe identifiers. */
public record SuscripcionResumenDto(TitularSuscripcion origen, String planCodigo, String planNombre, String estado, boolean activa,
                                    Instant finPrueba, Instant finPeriodoActual, boolean cancelaAlFinal,
                                    Instant ultimoPagoFecha, BigDecimal ultimoPagoMonto, String ultimoPagoMoneda,
                                    String ultimoPagoEstado) { }
