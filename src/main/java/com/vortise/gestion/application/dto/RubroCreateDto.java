package com.vortise.gestion.application.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDate;

public record RubroCreateDto(
    String nRubro,
    String rubro,
    String nivel,
    Integer subNumeroRubro,
    String subRubro,
    Double cantidad,
    String unidad,
    Double productividad,
    String empresa,
    String tipoContratista,
    Long subcontratistaId,
    Double horasOficialesPrevistas,
    Double horasAyudantesPrevistas,
    Integer cantidadPersonas,
    LocalDate fechaInicioPlanificada,
    LocalDate fechaFinPlanificada,
    boolean cronogramaManual,
    Long id,
    Double rendimientoOficial,
    Double rendimientoAyudante,
    @JsonDeserialize(using = RubroCreateDto.FrentesDeserializer.class) Integer frentes
) {
    public RubroCreateDto(String nRubro, String rubro, String nivel, Integer subNumeroRubro,
            String subRubro, Double cantidad, String unidad, Double productividad, String empresa,
            String tipoContratista, Long subcontratistaId, Double horasOficialesPrevistas,
            Double horasAyudantesPrevistas, Integer cantidadPersonas, LocalDate fechaInicioPlanificada,
            LocalDate fechaFinPlanificada, boolean cronogramaManual, Long id,
            Double rendimientoOficial, Double rendimientoAyudante) {
        this(nRubro, rubro, nivel, subNumeroRubro, subRubro, cantidad, unidad, productividad, empresa,
            tipoContratista, subcontratistaId, horasOficialesPrevistas, horasAyudantesPrevistas,
            cantidadPersonas, fechaInicioPlanificada, fechaFinPlanificada, cronogramaManual, id,
            rendimientoOficial, rendimientoAyudante, null);
    }

    public static class FrentesDeserializer extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(Integer.class, "Frentes debe ser un número entero");
            }
            return parser.getIntValue();
        }
    }
}