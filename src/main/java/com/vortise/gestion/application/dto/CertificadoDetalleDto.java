package com.vortise.gestion.application.dto;

import java.time.LocalDate;
import java.util.List;

public record CertificadoDetalleDto(
    Long id, Integer numero, String etiqueta, String periodicidad, LocalDate periodoDesde, LocalDate fechaCorte, String estado,
    Long subcontratistaId, String empresa, boolean propia, boolean tieneExcedente, boolean tieneAjuste, List<CertificadoFilaDto> filas
) { }
