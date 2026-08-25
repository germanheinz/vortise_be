/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.domain.service;

import com.vortise.gestion.domain.model.Rendimiento;
import com.vortise.gestion.domain.model.StatusTarea;
import com.vortise.gestion.domain.model.Tarea;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CostoCalculadorService {
    public double calcularCosto(double cantidadM2, Rendimiento rendimiento) {
        return cantidadM2 * rendimiento.getHhPorM2() * rendimiento.getPrecioPorHh();
    }

    public StatusTarea derivarStatus(int avancePct) {
        if (avancePct == 0) {
            return StatusTarea.PENDIENTE;
        }
        if (avancePct == 100) {
            return StatusTarea.EJECUTADO;
        }
        return StatusTarea.EN_PROCESO;
    }

    public double sumarCostosTareas(List<Tarea> tareas) {
        return tareas.stream().filter(t -> t.getCosto() != null).mapToDouble(Tarea::getCosto).sum();
    }
}
