package com.vortise.gestion.application.helper;

import com.vortise.gestion.application.service.GetPrevistoRealUseCase.BaseAnexo;
import com.vortise.gestion.application.service.GetPrevistoRealUseCase.Load;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.RubroObra;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public final class CalculoAnexoPrevistoReal {
    private CalculoAnexoPrevistoReal() {
    }

    public static BaseAnexo agregar(List<Load> cargas) {
        var anexos = cargas.stream().map(Load::baseAnexo).filter(Objects::nonNull).toList();
        if (anexos.isEmpty()) {
            return null;
        }
        Double horasPrevistasFecha = null;
        Double horasPrevistasHecho = null;
        Double horasReales = null;
        for (BaseAnexo anexo : anexos) {
            horasPrevistasFecha = sumar(horasPrevistasFecha, anexo.hhPrevistasFecha());
            horasPrevistasHecho = sumar(horasPrevistasHecho, anexo.hhPrevistasHecho());
            horasReales = sumar(horasReales, anexo.hhReales());
        }
        return new BaseAnexo(null, null, horasPrevistasFecha, horasPrevistasHecho, horasReales);
    }

    private static Double sumar(Double a, Double b) {
        if (a == null || !Double.isFinite(a)) return b;
        if (b == null || !Double.isFinite(b)) return a;
        double suma = a + b;
        return Double.isFinite(suma) ? suma : null;
    }

    public static BaseAnexo calcular(RubroObra rubro, ObraTarea tarea, Double jornada, Integer personas) {
        Integer diasTarea = tarea.getDias();
        Double horasPrevistasFecha = null;
        Integer diasUsados = null;
        boolean tieneDotacionYJornada = personas != null && personas > 0 && noNegativo(jornada) && jornada > 0;

        if (rubro.getFechaInicioPlanificada() != null) {
            LocalDate hoy = LocalDate.now();
            long dias = ChronoUnit.DAYS.between(rubro.getFechaInicioPlanificada(), hoy);
            if (dias > 0 && tieneDotacionYJornada) {
                horasPrevistasFecha = finito(personas * jornada * dias);
                diasUsados = (int) dias;
            }
        }

        Double cantidadReal = tarea.getCantidadReal();
        Double cantidadPrevista = rubro.getCantidad();
        Double horasOficialesPrevistas = rubro.getHorasOficialesPrevistas();
        Double horasAyudantesPrevistas = rubro.getHorasAyudantesPrevistas();
        Double horasPrevistasHecho = calcularHorasPrevistasHecho(
                cantidadReal, cantidadPrevista, horasOficialesPrevistas, horasAyudantesPrevistas);

        Double horasOficiales = tarea.getHorasOficiales();
        Double horasAyudantes = tarea.getHorasAyudantes();
        Double horasReales = noNegativo(horasOficiales) && noNegativo(horasAyudantes)
                ? finito(horasOficiales + horasAyudantes)
                : null;

        if (horasPrevistasFecha == null && tieneDotacionYJornada && diasTarea != null && diasTarea >= 0) {
            horasPrevistasFecha = finito(personas * jornada * diasTarea);
            diasUsados = diasTarea;
        }
        if (horasPrevistasFecha == null && noNegativo(horasOficialesPrevistas) && noNegativo(horasAyudantesPrevistas)) {
            horasPrevistasFecha = finito(horasOficialesPrevistas + horasAyudantesPrevistas);
        }

        return new BaseAnexo(diasUsados, personas, horasPrevistasFecha, horasPrevistasHecho, horasReales);
    }

    private static Double calcularHorasPrevistasHecho(
            Double cantidadReal, Double cantidadPrevista, Double horasOficiales, Double horasAyudantes) {
        if (!noNegativo(cantidadReal) || !noNegativo(cantidadPrevista) || cantidadPrevista <= 0
                || !noNegativo(horasOficiales) || !noNegativo(horasAyudantes)) {
            return null;
        }
        return finito(cantidadReal * ((horasOficiales + horasAyudantes) / cantidadPrevista));
    }

    private static boolean noNegativo(Double valor) {
        return valor != null && Double.isFinite(valor) && valor >= 0;
    }

    private static Double finito(double valor) {
        return Double.isFinite(valor) ? valor : null;
    }
}
