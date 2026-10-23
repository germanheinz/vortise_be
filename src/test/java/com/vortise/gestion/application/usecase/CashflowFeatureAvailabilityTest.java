package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.service.CashflowCalculadorService;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CashflowFeatureAvailabilityTest {
    @Test
    void disabledIndirectCostsRejectCashflowBeforeReadingGlobalCosts() {
        ProyectoRepository proyectos = org.mockito.Mockito.mock(ProyectoRepository.class);
        PresupuestoLineaRepository lineas = org.mockito.Mockito.mock(PresupuestoLineaRepository.class);
        GastoFijoRepository gastos = org.mockito.Mockito.mock(GastoFijoRepository.class);
        IngresoRepository ingresos = org.mockito.Mockito.mock(IngresoRepository.class);
        CashflowCalculadorService calculador = org.mockito.Mockito.mock(CashflowCalculadorService.class);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false, false);
        GetCashflowUseCase useCase = new GetCashflowUseCase(proyectos, lineas, gastos, ingresos, calculador, policy);

        assertThrows(FeatureUnavailableException.class, () -> useCase.execute(12L));
        verify(proyectos, never()).findById(12L);
        verify(gastos, never()).findAll();
        verify(lineas, never()).findByProyectoId(12L);
        verify(ingresos, never()).findByProyectoId(12L);
        verify(calculador, never()).calcular(org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
    }
}
