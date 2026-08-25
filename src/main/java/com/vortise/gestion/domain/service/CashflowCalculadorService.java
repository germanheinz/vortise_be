/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.domain.service;

import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CashflowCalculadorService {
    public List<MesCashflow> calcular(List<PresupuestoLinea> lineas, List<GastoFijo> gastosFijos, List<Ingreso> ingresos) {
        int maxMes = this.calcularMaxMes(lineas, gastosFijos, ingresos);
        Map<Integer, Double> ingresosPorMes = ingresos.stream().filter(i -> i.getMes() != null).collect(Collectors.groupingBy(Ingreso::getMes, Collectors.summingDouble(Ingreso::getMonto)));
        Map<Integer, Double> costoDirectoPorMes = lineas.stream().filter(l -> l.getMes() != null).collect(Collectors.groupingBy(PresupuestoLinea::getMes, Collectors.summingDouble(l -> {
            if (l.getM2() == null || l.getHhPorM2() == null || l.getPrecioPorHh() == null) {
                return 0.0;
            }
            return l.getM2() * l.getHhPorM2() * l.getPrecioPorHh();
        })));
        HashMap<Integer, Double> costoIndirectoPorMes = new HashMap<Integer, Double>();
        for (GastoFijo g : gastosFijos) {
            if (g.getMes() == null) {
                for (int m = 1; m <= maxMes; ++m) {
                    costoIndirectoPorMes.merge(m, g.getMonto(), Double::sum);
                }
                continue;
            }
            costoIndirectoPorMes.merge(g.getMes(), g.getMonto(), Double::sum);
        }
        double acumulado = 0.0;
        ArrayList<MesCashflow> resultado = new ArrayList<MesCashflow>();
        for (int mes = 1; mes <= maxMes; ++mes) {
            double ing = ingresosPorMes.getOrDefault(mes, 0.0);
            double cd = costoDirectoPorMes.getOrDefault(mes, 0.0);
            double ci = costoIndirectoPorMes.getOrDefault(mes, 0.0);
            double balance = ing - cd - ci;
            resultado.add(new MesCashflow(mes, ing, cd, ci, balance, acumulado += balance));
        }
        return resultado;
    }

    private int calcularMaxMes(List<PresupuestoLinea> lineas, List<GastoFijo> gastosFijos, List<Ingreso> ingresos) {
        int max = 12;
        for (PresupuestoLinea l : lineas) {
            if (l.getMes() == null || l.getMes() <= max) continue;
            max = l.getMes();
        }
        for (GastoFijo g : gastosFijos) {
            if (g.getMes() == null || g.getMes() <= max) continue;
            max = g.getMes();
        }
        for (Ingreso i : ingresos) {
            if (i.getMes() == null || i.getMes() <= max) continue;
            max = i.getMes();
        }
        return max;
    }

    public record MesCashflow(int mes, double ingresos, double costoDirecto, double costoIndirecto, double balance, double acumulado) {
    }
}
