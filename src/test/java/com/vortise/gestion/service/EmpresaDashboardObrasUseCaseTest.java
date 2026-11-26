package com.vortise.gestion.service;

import com.vortise.gestion.application.helper.PrevistoRealCalculator;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmpresaDashboardObrasUseCaseTest {
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final RestriccionObraRepository restricciones = mock(RestriccionObraRepository.class);
    private final PrevistoRealService previstoReal = mock(PrevistoRealService.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final EmpresaConsultaService useCase = new EmpresaConsultaService(empresas, proyectos, restricciones, previstoReal, identity, null, null);

    private final Proyecto obra = new Proyecto();

    private RubroObra rubro(long id, double oficiales, double ayudantes, String contratista) {
        var rubro = new RubroObra();
        rubro.setId(id); rubro.setProyecto(obra); rubro.setTipoContratista(contratista);
        rubro.setHorasOficialesPrevistas(oficiales); rubro.setHorasAyudantesPrevistas(ayudantes);
        return rubro;
    }

    private static PrevistoRealService.Activity activity(long rubroId, Double desvio, Double proyeccion) {
        var metrics = new PrevistoRealCalculator.Metrics(null, null, null, null, null, null, desvio, proyeccion, null, null, null);
        return new PrevistoRealService.Activity(rubroId, "A" + rubroId, "m2", null, null, null, metrics, List.of());
    }

    private static RestriccionObra restriccion(Proyecto proyecto, String estado) {
        var item = new RestriccionObra(); item.setProyecto(proyecto); item.setEstado(estado);
        return item;
    }

    private void givenObra(Double costoOficial, Double costoAyudante, PrevistoRealService.Activity... actividades) {
        obra.setId(5L); obra.setNombre("Obra A");
        when(empresas.findById(1L)).thenReturn(Optional.of(new Empresa("Sintética", null)));
        when(proyectos.findByEmpresaRelacionadaId(1L)).thenReturn(List.of(obra));
        when(previstoReal.execute(5L)).thenReturn(new PrevistoRealService.Summary(costoOficial, costoAyudante, 8., 0, List.of(actividades)));
    }

    @Test
    void sumsOwnLaborDeviationsAndComputesPercentagesOverTheBudgetedLaborCost() {
        obra.setRubros(List.of(rubro(10, 100, 50, "PROPIA"), rubro(11, 100, 0, null), rubro(12, 999, 999, "SUBCONTRATO")));
        givenObra(10., 5., activity(10, -300., -100.), activity(11, 100., 50.), activity(12, 5000., 5000.));
        var item = useCase.dashboardObras(1L).obras().get(0);
        assertEquals(-200., item.desvioActual(), "the subcontracted activity is excluded");
        assertEquals(-50., item.desvioProyectado());
        assertEquals(2250., item.costoManoObraPrevisto(), "(100*10+50*5) + (100*10)");
        assertEquals(-200. / 2250. * 100, item.porcentajeActual(), 1e-9);
        assertEquals(-50. / 2250. * 100, item.porcentajeProyectado(), 1e-9);
        assertEquals(2, item.actividadesPropias());
        assertEquals(2, item.actividadesConDatos());
    }

    @Test
    void countsOnlyRestrictionsThatAreNotResolvedOrReleased() {
        obra.setRubros(List.of());
        givenObra(10., 5.);
        var otra = new Proyecto(); otra.setId(6L);
        when(restricciones.findByProyectoEmpresaRelacionadaIdOrderByIdAsc(1L)).thenReturn(List.of(
            restriccion(obra, "PENDIENTE"), restriccion(obra, null), restriccion(obra, " resuelto "), restriccion(obra, "LIBERADO"), restriccion(otra, "PENDIENTE")));
        assertEquals(2, useCase.dashboardObras(1L).obras().get(0).restriccionesActivas());
    }

    @Test
    void withoutDataOrRatesAmountsAndPercentagesAreNullInsteadOfZero() {
        obra.setRubros(List.of(rubro(10, 100, 50, "PROPIA")));
        givenObra(null, 5., activity(10, null, null));
        var item = useCase.dashboardObras(1L).obras().get(0);
        assertNull(item.desvioActual()); assertNull(item.desvioProyectado());
        assertNull(item.costoManoObraPrevisto()); assertNull(item.porcentajeActual());
        assertEquals(0, item.actividadesConDatos());
    }

    @Test
    void deniedCompanyReadsNothing() {
        doThrow(new AccessDeniedException("No tienes acceso a esta empresa")).when(identity).requireEmpresa(2L);
        assertThrows(AccessDeniedException.class, () -> useCase.dashboardObras(2L));
        verifyNoInteractions(empresas, proyectos, restricciones, previstoReal);
    }
}
