package com.vortise.gestion.application.mapper;

import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.RubroObra;

public final class ObraTareaMapper {
    private ObraTareaMapper() {
    }

    public static ObraTareaDto toDto(ObraTarea tarea) {
        var rubroBase = tarea.getProyecto().getRubros() == null ? null : tarea.getProyecto().getRubros().stream()
            .filter(item -> item.getId() != null && item.getId().equals(tarea.getRubroId()))
            .findFirst().orElse(null);
        Double horasOficiales = tarea.getHorasOficiales();
        Double horasAyudantes = tarea.getHorasAyudantes();
        Double cantidadReal = tarea.getCantidadReal();
        double horasTotales = (horasOficiales == null ? 0 : horasOficiales)
                + (horasAyudantes == null ? 0 : horasAyudantes);

        Double productividadPresupuesto = productividadPresupuesto(rubroBase);
        Double cantidadPrevista = productividadPresupuesto != null && productividadPresupuesto > 0
                && horasOficiales != null && horasAyudantes != null
                ? horasTotales / productividadPresupuesto
                : null;
        Double cumplimiento = cantidadPrevista != null && cantidadPrevista > 0 && cantidadReal != null
                ? redondear(cantidadReal / cantidadPrevista * 100)
                : null;
        Double productividadReal = cantidadReal == null || cantidadReal == 0
                || (horasOficiales == null && horasAyudantes == null)
                ? null
                : redondear(horasTotales / cantidadReal);
        Double rendimiento = productividadPresupuesto == null || productividadReal == null || productividadPresupuesto == 0
                ? null
                : redondear(productividadReal / productividadPresupuesto);
        Double desvio = rendimiento == null ? null : redondear((rendimiento - 1) * 100);
        return new ObraTareaDto(
            tarea.getId(),
            tarea.getProyecto().getId(),
            tarea.getNombre(),
            tarea.getSector(),
            tarea.getNivel(),
            tarea.getUnidad(),
            tarea.getRubro(),
            rubroBase == null ? tarea.getPorcentajeCumplimiento() : cumplimiento,
            tarea.getCumplimiento(),
            tarea.getCausaNoCumplimiento(),
            tarea.getComentario(),
            tarea.getHorasOficiales(),
            tarea.getHorasAyudantes(),
            rubroBase == null ? tarea.getCantidadPrevista() : cantidadPrevista,
            tarea.getCantidadReal(),
            tarea.getMedidaCorrectiva(),
            tarea.getDescripcion(),
            tarea.getFecha(),
            productividadPresupuesto,
            productividadReal,
            rendimiento,
            desvio,
            tarea.getRubroId(),
            tarea.getDias(),
            tarea.isDesencofrado()
        );
    }

    private static Double productividadPresupuesto(RubroObra rubro) {
        if (rubro == null || rubro.getCantidad() == null || rubro.getCantidad() <= 0
                || rubro.getHorasOficialesPrevistas() == null || rubro.getHorasAyudantesPrevistas() == null) {
            return null;
        }
        return (rubro.getHorasOficialesPrevistas() + rubro.getHorasAyudantesPrevistas()) / rubro.getCantidad();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
