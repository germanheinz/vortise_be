package com.vortise.gestion.application.dto;

import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores;
import java.util.List;

/** Response of {@code GET /api/proyectos/{id}/indicadores}: every indicator level of spec 021, unrounded. */
public record IndicadoresObraDto(CalculadoraIndicadores.Parametros parametros, long tareasSinVincular,
        CalculadoraIndicadores.Obra obra, List<CalculadoraIndicadores.Rubro> rubros) { }
