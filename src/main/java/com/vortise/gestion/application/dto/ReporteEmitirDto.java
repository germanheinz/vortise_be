package com.vortise.gestion.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

/** The snapshot is the report data computed by the client at issue time; it is stored as is and rendered read-only. */
public record ReporteEmitirDto(
    @NotNull LocalDate fechaCorte,
    @NotNull LocalDate semanaDesde,
    @NotNull LocalDate semanaHasta,
    @NotNull JsonNode snapshot
) { }
