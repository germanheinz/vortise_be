package com.vortise.gestion.application.dto;

public record RestriccionUpdateDto(
    String prioridad,
    String estado,
    Integer orden
) {
}