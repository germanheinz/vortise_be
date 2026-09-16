package com.vortise.gestion.application.dto;

import java.time.LocalDate;

public record RubroDto(
    Long id,
    Long proyectoId,
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
    String subcontratistaNombre,
    Double horasOficialesPrevistas,
    Double horasAyudantesPrevistas,
    Integer cantidadPersonas,
    LocalDate fechaInicioPlanificada,
    LocalDate fechaFinPlanificada,
    boolean cronogramaManual
) {
}