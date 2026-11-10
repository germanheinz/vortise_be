package com.vortise.gestion.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record ReporteDetalleDto(
    Long id, Integer numero, LocalDate semanaDesde, LocalDate semanaHasta, LocalDate fechaCorte,
    String emitidoPor, LocalDateTime emitidoEn, Integer versionEsquema, JsonNode snapshot, List<ReporteImagenDto> imagenes
) { }
