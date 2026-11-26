package com.vortise.gestion.application.dto;

import java.util.List;

/** Per-obra deviation snapshot for the company dashboard, taken from the project indicators (spec 021). Own labour only. */
public record EmpresaDashboardObrasDto(Long empresaId, List<Obra> obras) {
    /**
     * Current deviation is relative to the planned cost of the work done; the projected one is the deviation plus
     * projection at completion over the labour budget. Percentages are ×100 and null without data.
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
