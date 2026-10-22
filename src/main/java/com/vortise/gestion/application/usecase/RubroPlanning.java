package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.domain.model.RubroObra;

final class RubroPlanning {
    private RubroPlanning() { }
    static void apply(RubroObra rubro, RubroCreateDto dto) {
        for (Double n : new Double[]{dto.cantidad(), dto.horasOficialesPrevistas(), dto.horasAyudantesPrevistas(), dto.productividad(), dto.rendimientoOficial(), dto.rendimientoAyudante()}) {
            if (n != null && (!Double.isFinite(n) || n < 0)) throw new IllegalArgumentException("Cantidad y horas previstas inválidas");
        }
        if (dto.fechaInicioPlanificada() != null && dto.fechaFinPlanificada() != null && dto.fechaFinPlanificada().isBefore(dto.fechaInicioPlanificada())) {
            throw new IllegalArgumentException("Fin de actividad anterior al inicio");
        }
        if (dto.rendimientoOficial() == null && dto.rendimientoAyudante() == null) {
            if (dto.cantidad() == null || dto.cantidad() <= 0
                || dto.horasOficialesPrevistas() == null || dto.horasAyudantesPrevistas() == null) return;
            double officialRate = dto.horasOficialesPrevistas() / dto.cantidad();
            double helperRate = dto.horasAyudantesPrevistas() / dto.cantidad();
            if (!Double.isFinite(officialRate) || !Double.isFinite(helperRate)) throw new IllegalArgumentException("Rendimiento fuera de rango");
            rubro.setRendimientoOficial(officialRate);
            rubro.setRendimientoAyudante(helperRate);
            return;
        }
        for (Double rate : new Double[]{dto.rendimientoOficial(), dto.rendimientoAyudante()}) {
            if (rate == null || !Double.isFinite(rate) || rate < 0
                || (dto.cantidad() != null && dto.cantidad() > 0 && !Double.isFinite(rate * dto.cantidad()))) {
                throw new IllegalArgumentException("Ingresá ambos rendimientos no negativos");
            }
        }
        double total = dto.rendimientoOficial() + dto.rendimientoAyudante();
        if (!Double.isFinite(total) || (dto.cantidad() != null && dto.cantidad() > 0 && !Double.isFinite(total * dto.cantidad()))) {
            throw new IllegalArgumentException("Rendimiento fuera de rango");
        }
        rubro.setRendimientoOficial(dto.rendimientoOficial());
        rubro.setRendimientoAyudante(dto.rendimientoAyudante());
        if (dto.cantidad() == null || dto.cantidad() <= 0) {
            rubro.setHorasOficialesPrevistas(null);
            rubro.setHorasAyudantesPrevistas(null);
            rubro.setProductividad(total);
            return;
        }
        rubro.setHorasOficialesPrevistas(dto.rendimientoOficial() * dto.cantidad());
        rubro.setHorasAyudantesPrevistas(dto.rendimientoAyudante() * dto.cantidad());
        rubro.setProductividad(total);
    }
}
