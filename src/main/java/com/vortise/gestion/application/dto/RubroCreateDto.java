package com.vortise.gestion.application.dto;

public record RubroCreateDto(
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