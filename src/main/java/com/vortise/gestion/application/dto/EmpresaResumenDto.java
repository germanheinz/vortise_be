package com.vortise.gestion.application.dto;

/** Company-wide totals so clients do not need one request per obra. */
public record EmpresaResumenDto(
    Long empresaId,
    long obras,
    long restricciones,
    double horasRegistradas,
    long cargasVinculadas,
    long cargasCumplidas
) {
}
