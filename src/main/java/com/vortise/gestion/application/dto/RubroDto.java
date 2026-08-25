package com.vortise.gestion.application.dto;

public record RubroDto(
    Long id,
    Long proyectoId,
    Integer nRubro,
    String rubro,
    Integer subNumeroRubro,
    String subRubro,
    Double cantidad,
    String unidad,
    Double productividad,
    String empresa
) {
}