package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DashboardTenantAuthorizationTest {
    @Mock private ProyectoRepository proyectos;
    @Mock private TareaRepository tareas;
    @Mock private IngresoRepository ingresos;
    @Mock private GastoFijoRepository gastos;
    @Mock private PresupuestoLineaRepository presupuesto;
    @Mock private CurrentTenantService tenant;
    @Mock private Proyecto proyecto;
    @Mock private Empresa empresa;

    @Test
    void deniesDashboardBeforeLoadingFinancialDataForUnauthorizedTenant() {
        when(proyectos.findById(42L)).thenReturn(java.util.Optional.of(proyecto));
        when(proyecto.getEmpresaRelacionada()).thenReturn(empresa);
        when(empresa.getId()).thenReturn(7L);
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied")).when(tenant).requireEmpresa(7L);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false, false);
        GetDashboardUseCase useCase = new GetDashboardUseCase(proyectos, tareas, ingresos, gastos, presupuesto, tenant, policy);

        assertThrows(AccessDeniedException.class, () -> useCase.execute(42L));
        verify(tareas, never()).findByEntregaProyectoId(42L);
        verify(ingresos, never()).findByProyectoId(42L);
        verify(gastos, never()).findAll();
        verify(presupuesto, never()).findByProyectoId(42L);
    }

    @Test
    void reportsGlobalCostAndBalanceUnavailableWithoutReadingGlobalCosts() {
        when(proyectos.findById(42L)).thenReturn(java.util.Optional.of(proyecto));
        when(proyecto.getEmpresaRelacionada()).thenReturn(empresa);
        when(proyecto.getId()).thenReturn(42L);
        when(empresa.getId()).thenReturn(7L);
        when(tareas.findByEntregaProyectoId(42L)).thenReturn(java.util.List.of());
        when(ingresos.findByProyectoId(42L)).thenReturn(java.util.List.of());
        when(presupuesto.findByProyectoId(42L)).thenReturn(java.util.List.of());
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false, false);
        GetDashboardUseCase useCase = new GetDashboardUseCase(proyectos, tareas, ingresos, gastos, presupuesto, tenant, policy);

        var dashboard = useCase.execute(42L);

        verify(tenant).requireEmpresa(7L);
        verify(gastos, never()).findAll();
        org.junit.jupiter.api.Assertions.assertFalse(dashboard.kpis().costoIndirectoDisponible());
        org.junit.jupiter.api.Assertions.assertNull(dashboard.kpis().costoIndirectoArs());
        org.junit.jupiter.api.Assertions.assertNull(dashboard.kpis().balanceArs());
    }
}
