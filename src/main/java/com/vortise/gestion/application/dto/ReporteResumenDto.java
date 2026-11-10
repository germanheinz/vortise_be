package com.vortise.gestion.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReporteResumenDto(
    Long id, Integer numero, LocalDate semanaDesde, LocalDate semanaHasta, LocalDate fechaCorte,
    String emitidoPor, LocalDateTime emitidoEn, int cantidadImagenes
) { }
