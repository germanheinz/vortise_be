/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CashflowDto;
import com.vortise.gestion.application.dto.CashflowMesDto;
import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.FeaturePolicy;
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
    private final FeaturePolicy featurePolicy;

    public CashflowDto execute(Long proyectoId) {
        if (!featurePolicy.gastosFijosEnabled()) {
            throw new FeatureUnavailableException("El cashflow está temporalmente deshabilitado hasta completar el aislamiento de gastos indirectos por empresa.");
        }
        Proyecto proyecto = this.proyectoRepository.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado: " + proyectoId));
        List<PresupuestoLinea> lineas = this.presupuestoLineaRepository.findByProyectoId(proyectoId);
        List<GastoFijo> gastosFijos = this.gastoFijoRepository.findAll();
        List<Ingreso> ingresos = this.ingresoRepository.findByProyectoId(proyectoId);
        List<CashflowMesDto> meses = this.cashflowCalculadorService.calcular(lineas, gastosFijos, ingresos).stream().map(m -> new CashflowMesDto(m.mes(), m.ingresos(), m.costoDirecto(), m.costoIndirecto(), m.balance(), m.acumulado())).toList();
        return new CashflowDto(proyectoId, proyecto.getNombre(), meses);
    }

        public GetCashflowUseCase(ProyectoRepository proyectoRepository, PresupuestoLineaRepository presupuestoLineaRepository,
            GastoFijoRepository gastoFijoRepository, IngresoRepository ingresoRepository,
            CashflowCalculadorService cashflowCalculadorService, FeaturePolicy featurePolicy) {
        this.proyectoRepository = proyectoRepository;
        this.presupuestoLineaRepository = presupuestoLineaRepository;
        this.gastoFijoRepository = gastoFijoRepository;
        this.ingresoRepository = ingresoRepository;
        this.cashflowCalculadorService = cashflowCalculadorService;
        this.featurePolicy = featurePolicy;
    }
}
