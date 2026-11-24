package com.vortise.gestion.application.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ObraTareaCreateDto(
    @NotBlank String nombre,
    String sector,
    String nivel,
    String unidad,
    String rubro,
    Double porcentajeCumplimiento,
    String cumplimiento,
    String causaNoCumplimiento,
    String comentario,
    Double horasOficiales,
    Double horasAyudantes,
    Double cantidadPrevista,
    Double cantidadReal,
    String medidaCorrectiva,
    String descripcion,
    LocalDate fecha,
    Long rubroId,
    /** Días laborables que abarca la carga, contados hacia atrás desde la fecha; sin valor se toma 1. */
    @Min(1) @Max(ObraTareaCreateDto.MAX_DIAS) Integer dias,
    /** Carga de horas de desencofrado: solo imputa horas, por lo que no admite cantidad ejecutada. */
    Boolean desencofrado
) {
    public static final int MAX_DIAS = 31;

    public ObraTareaCreateDto(String nombre, String sector, String nivel, String unidad, String rubro, Double porcentajeCumplimiento,
            String cumplimiento, String causaNoCumplimiento, String comentario, Double horasOficiales, Double horasAyudantes,
            Double cantidadPrevista, Double cantidadReal, String medidaCorrectiva, String descripcion, LocalDate fecha, Long rubroId) {
        this(nombre, sector, nivel, unidad, rubro, porcentajeCumplimiento, cumplimiento, causaNoCumplimiento, comentario, horasOficiales,
            horasAyudantes, cantidadPrevista, cantidadReal, medidaCorrectiva, descripcion, fecha, rubroId, null, null);
    }

    public ObraTareaCreateDto(String nombre, String sector, String nivel, String unidad, String rubro, Double porcentajeCumplimiento,
            String cumplimiento, String causaNoCumplimiento, String comentario, Double horasOficiales, Double horasAyudantes,
            Double cantidadPrevista, Double cantidadReal, String medidaCorrectiva, String descripcion, LocalDate fecha, Long rubroId, Integer dias) {
        this(nombre, sector, nivel, unidad, rubro, porcentajeCumplimiento, cumplimiento, causaNoCumplimiento, comentario, horasOficiales,
            horasAyudantes, cantidadPrevista, cantidadReal, medidaCorrectiva, descripcion, fecha, rubroId, dias, null);
    }
}