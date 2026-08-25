/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CashflowDto;
import com.vortise.gestion.application.dto.CashflowMesDto;
import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.service.CashflowCalculadorService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetCashflowUseCase {
    private final ProyectoRepository proyectoRepository;
    private final PresupuestoLineaRepository presupuestoLineaRepository;
    private final GastoFijoRepository gastoFijoRepository;
    private final IngresoRepository ingresoRepository;
    private final CashflowCalculadorService cashflowCalculadorService;

    public CashflowDto execute(Long proyectoId) {
        Proyecto proyecto = this.proyectoRepository.findById(proyectoId).orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));
        List<PresupuestoLinea> lineas = this.presupuestoLineaRepository.findByProyectoId(proyectoId);
        List<GastoFijo> gastosFijos = this.gastoFijoRepository.findAll();
        List<Ingreso> ingresos = this.ingresoRepository.findByProyectoId(proyectoId);
        List<CashflowMesDto> meses = this.cashflowCalculadorService.calcular(lineas, gastosFijos, ingresos).stream().map(m -> new CashflowMesDto(m.mes(), m.ingresos(), m.costoDirecto(), m.costoIndirecto(), m.balance(), m.acumulado())).toList();
        return new CashflowDto(proyectoId, proyecto.getNombre(), meses);
    }

    public GetCashflowUseCase(ProyectoRepository proyectoRepository, PresupuestoLineaRepository presupuestoLineaRepository, GastoFijoRepository gastoFijoRepository, IngresoRepository ingresoRepository, CashflowCalculadorService cashflowCalculadorService) {
        this.proyectoRepository = proyectoRepository;
        this.presupuestoLineaRepository = presupuestoLineaRepository;
        this.gastoFijoRepository = gastoFijoRepository;
        this.ingresoRepository = ingresoRepository;
        this.cashflowCalculadorService = cashflowCalculadorService;
    }
}
