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
    @JsonDeserialize(using = RubroCreateDto.FrentesDeserializer.class) Integer frentes,
    /** RUBRO | NIVEL | SUBRUBRO. Null on legacy payloads: the kind is inferred from the code. */
    String tipoNodo,
    /** Hierarchical code of the parent node in the same request; null on root rubros. */
    String parentCodigo,
    /** Only on root rubros: RUBRO_SUBRUBRO | RUBRO_NIVEL_SUBRUBRO | RUBRO_SUBRUBRO_NIVEL. */
    String varianteEstructura,
    String nombre,
    /** Only on root rubros: id of an existing contract of the project. */
    Long contratoId
) {
    public RubroCreateDto(String nRubro, String rubro, String nivel, Integer subNumeroRubro,
            String subRubro, Double cantidad, String unidad, Double productividad, String empresa,
            String tipoContratista, Long subcontratistaId, Double horasOficialesPrevistas,
            Double horasAyudantesPrevistas, Integer cantidadPersonas, LocalDate fechaInicioPlanificada,
            LocalDate fechaFinPlanificada, boolean cronogramaManual, Long id,
            Double rendimientoOficial, Double rendimientoAyudante, Integer frentes, String tipoNodo,
            String parentCodigo, String varianteEstructura, String nombre) {
        this(nRubro, rubro, nivel, subNumeroRubro, subRubro, cantidad, unidad, productividad, empresa,
            tipoContratista, subcontratistaId, horasOficialesPrevistas, horasAyudantesPrevistas,
            cantidadPersonas, fechaInicioPlanificada, fechaFinPlanificada, cronogramaManual, id,
            rendimientoOficial, rendimientoAyudante, frentes, tipoNodo, parentCodigo, varianteEstructura, nombre, null);
    }


    public RubroCreateDto(String nRubro, String rubro, String nivel, Integer subNumeroRubro,
            String subRubro, Double cantidad, String unidad, Double productividad, String empresa,
            String tipoContratista, Long subcontratistaId, Double horasOficialesPrevistas,
            Double horasAyudantesPrevistas, Integer cantidadPersonas, LocalDate fechaInicioPlanificada,
            LocalDate fechaFinPlanificada, boolean cronogramaManual, Long id,
            Double rendimientoOficial, Double rendimientoAyudante, Integer frentes) {
        this(nRubro, rubro, nivel, subNumeroRubro, subRubro, cantidad, unidad, productividad, empresa,
            tipoContratista, subcontratistaId, horasOficialesPrevistas, horasAyudantesPrevistas,
            cantidadPersonas, fechaInicioPlanificada, fechaFinPlanificada, cronogramaManual, id,
            rendimientoOficial, rendimientoAyudante, frentes, null, null, null, null, null);
    }

    public RubroCreateDto(String nRubro, String rubro, String nivel, Integer subNumeroRubro,
            String subRubro, Double cantidad, String unidad, Double productividad, String empresa,
            String tipoContratista, Long subcontratistaId, Double horasOficialesPrevistas,
            Double horasAyudantesPrevistas, Integer cantidadPersonas, LocalDate fechaInicioPlanificada,
            LocalDate fechaFinPlanificada, boolean cronogramaManual, Long id,
            Double rendimientoOficial, Double rendimientoAyudante) {
        this(nRubro, rubro, nivel, subNumeroRubro, subRubro, cantidad, unidad, productividad, empresa,
            tipoContratista, subcontratistaId, horasOficialesPrevistas, horasAyudantesPrevistas,
            cantidadPersonas, fechaInicioPlanificada, fechaFinPlanificada, cronogramaManual, id,
            rendimientoOficial, rendimientoAyudante, null, null, null, null, null, null);
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