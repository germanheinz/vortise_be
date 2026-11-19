package com.vortise.gestion.application.dto;

import java.util.List;

/** Per-obra deviation snapshot for the company dashboard. Amounts estimate own-labor cost only (subcontracts excluded). */
public record EmpresaDashboardObrasDto(Long empresaId, List<Obra> obras) {
    /**
     * Amounts are null when no own-labor activity has data. Percentages are relative to the budgeted labor cost
     * of the whole obra and are null when that cost cannot be computed (missing rates or planned hours).
     */
    public record Obra(
        Long id,
        String nombre,
        String numeroProyecto,
        Double desvioActual,
        Double desvioProyectado,
        Double porcentajeActual,
        Double porcentajeProyectado,
        Double costoManoObraPrevisto,
        int actividadesPropias,
        int actividadesConDatos,
        long restriccionesActivas
    ) { }
}
