package com.vortise.gestion.service;

import com.vortise.gestion.service.empresa.EmpresaConsultaService;
import com.vortise.gestion.service.proyecto.IndicadoresObraService;
import com.vortise.gestion.application.dto.IndicadoresObraDto;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.*;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.*;
import com.vortise.gestion.repository.*;
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
    private final IndicadoresObraService indicadores = mock(IndicadoresObraService.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final EmpresaConsultaService useCase = new EmpresaConsultaService(empresas, proyectos, restricciones, indicadores, identity, null, null);

    private final Proyecto obra = new Proyecto();

    private static ResultadoSubrubro subrubro(boolean conAvance) {
        return new ResultadoSubrubro(1L, "Platea", "m3", conAvance, null, 0, 0, null, null, null, null, null, null, null,
            null, null, null, null, false, List.of());
    }

    private static Obra totales(Double desvio, Double desvioPorcentaje, Double proyeccionTotal, Double porcentajeTotal, Double presupuesto) {
        return new Obra(null, null, null, null, null, presupuesto, desvio, desvioPorcentaje, null, null, null,
            proyeccionTotal, porcentajeTotal, null, null);
    }

    private static RestriccionObra restriccion(Proyecto proyecto, String estado) {
        var item = new RestriccionObra(); item.setProyecto(proyecto); item.setEstado(estado);
        return item;
    }

    private void givenObra(Obra totales, ResultadoSubrubro... subrubros) {
        obra.setId(5L); obra.setNombre("Obra A");
        when(empresas.findById(1L)).thenReturn(Optional.of(new Empresa("Sintética", null)));
        when(proyectos.findByEmpresaRelacionadaId(1L)).thenReturn(List.of(obra));
        var piso = new Piso(null, null, null, List.of(subrubros));
        when(indicadores.calcular(5L)).thenReturn(new IndicadoresObraDto(new Parametros(10., 5., 8.), 0, totales,
            List.of(new CalculadoraIndicadores.Rubro(1L, "Hormigón", null, List.of(piso)))));
    }

    @Test
    void showsTheProjectIndicatorsWithPercentagesTimesOneHundred() {
        givenObra(totales(-135_000., -0.6, -405_000., -540_000. / 2_493_000, 2_493_000.), subrubro(true), subrubro(false));
        var item = useCase.dashboardObras(1L).obras().get(0);
        assertEquals(-135_000., item.desvioActual());
        assertEquals(-405_000., item.desvioProyectado());
        assertEquals(2_493_000., item.costoManoObraPrevisto());
        assertEquals(-60., item.porcentajeActual(), 1e-9);
        assertEquals(-540_000. / 2_493_000 * 100, item.porcentajeProyectado(), 1e-9);
        assertEquals(2, item.actividadesPropias());
        assertEquals(1, item.actividadesConDatos());
    }

    @Test
    void countsOnlyRestrictionsThatAreNotResolvedOrReleased() {
        givenObra(totales(null, null, null, null, null));
        var otra = new Proyecto(); otra.setId(6L);
        when(restricciones.findByProyectoEmpresaRelacionadaIdOrderByIdAsc(1L)).thenReturn(List.of(
            restriccion(obra, "PENDIENTE"), restriccion(obra, null), restriccion(obra, " resuelto "), restriccion(obra, "LIBERADO"), restriccion(otra, "PENDIENTE")));
        assertEquals(2, useCase.dashboardObras(1L).obras().get(0).restriccionesActivas());
    }

    @Test
    void withoutDataOrRatesAmountsAndPercentagesAreNullInsteadOfZero() {
        givenObra(totales(null, null, null, null, null), subrubro(false));
        var item = useCase.dashboardObras(1L).obras().get(0);
        assertNull(item.desvioActual()); assertNull(item.desvioProyectado());
        assertNull(item.costoManoObraPrevisto()); assertNull(item.porcentajeActual()); assertNull(item.porcentajeProyectado());
        assertEquals(0, item.actividadesConDatos());
    }

    @Test
    void deniedCompanyReadsNothing() {
        doThrow(new AccessDeniedException("No tienes acceso a esta empresa")).when(identity).requireEmpresa(2L);
        assertThrows(AccessDeniedException.class, () -> useCase.dashboardObras(2L));
        verifyNoInteractions(empresas, proyectos, restricciones, indicadores);
    }
}
