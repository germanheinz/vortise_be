package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RubroPlanningTest {
    private RubroCreateDto row(Long id, Double of, Double ay) {
        return new RubroCreateDto("5000","Hormigón",null,null,"Fundación",155.4,"m3",0.,null,"PROPIA",null,
            1.,2.,15,null,null,false,id,of,ay);
    }
    @Test void ratesDeriveHoursAndOverrideIncomingTotals() {
        var r = new RubroObra();
        RubroPlanning.apply(r,row(null,4000./155.4,1881.5/155.4));
        assertEquals(4000.,r.getHorasOficialesPrevistas(),1e-8);
        assertEquals(1881.5,r.getHorasAyudantesPrevistas(),1e-8);
        assertEquals(37.8474903475,r.getProductividad(),1e-8);
        assertEquals(4000. / 155.4,r.getRendimientoOficial(),1e-8);
        assertEquals(1881.5 / 155.4,r.getRendimientoAyudante(),1e-8);
    }
    @Test void rejectsPartialNegativeAndNonfiniteRates() {
        for (Double n : new Double[]{null,-1.,Double.NaN,Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,() -> RubroPlanning.apply(new RubroObra(),row(null,1.,n)));
        }
    }
    @Test void acceptsExplicitZeroHelperRate() {
        var r = new RubroObra(); RubroPlanning.apply(r,row(null,10.,0.));
        assertEquals(0.,r.getHorasAyudantesPrevistas());
        assertEquals(10.,r.getRendimientoOficial());
        assertEquals(0.,r.getRendimientoAyudante());
    }
    @Test void ratesAreStoredEvenWhenIncomingHoursAreAbsent() {
        var r = new RubroObra();
        var dto = new RubroCreateDto("5000","Hormigón",null,null,"Fundación",2.,"m3",null,null,"PROPIA",null,
            null,null,null,null,null,false,null,25.74,12.107);
        RubroPlanning.apply(r,dto);
        assertEquals(25.74,r.getRendimientoOficial());
        assertEquals(12.107,r.getRendimientoAyudante());
        assertEquals(51.48,r.getHorasOficialesPrevistas());
        assertEquals(24.214,r.getHorasAyudantesPrevistas());
    }
    @Test void ratesAreStoredWithoutQuantityAndHoursRemainUnavailable() {
        var r = new RubroObra();
        var dto = new RubroCreateDto("5000","Hormigón",null,null,"Fundación",null,"m3",null,null,"PROPIA",null,
            null,null,null,null,null,false,null,25.74,12.107);
        RubroPlanning.apply(r,dto);
        assertEquals(25.74,r.getRendimientoOficial());
        assertEquals(12.107,r.getRendimientoAyudante());
        assertNull(r.getHorasOficialesPrevistas());
        assertNull(r.getHorasAyudantesPrevistas());
    }
    @Test void ratesAreStoredWithZeroQuantityAndHoursRemainUnavailable() {
        var r = new RubroObra();
        var dto = new RubroCreateDto("5000","Hormigón",null,null,"Fundación",0.,"m3",null,null,"PROPIA",null,
            null,null,null,null,null,false,null,25.74,12.107);
        RubroPlanning.apply(r,dto);
        assertEquals(25.74,r.getRendimientoOficial());
        assertEquals(12.107,r.getRendimientoAyudante());
        assertNull(r.getHorasOficialesPrevistas());
        assertNull(r.getHorasAyudantesPrevistas());
    }
    @Test void omittedRatesKeepExistingRatesOnUpdate() {
        var r = new RubroObra();
        r.setRendimientoOficial(25.74);
        r.setRendimientoAyudante(12.107);
        var dto = new RubroCreateDto("5000","Hormigón",null,null,"Fundación",null,"m3",null,null,"PROPIA",null,
            null,null,null,null,null,false,1L,null,null);
        RubroPlanning.apply(r,dto);
        assertEquals(25.74,r.getRendimientoOficial());
        assertEquals(12.107,r.getRendimientoAyudante());
    }
    @Test void legacyHoursWithoutRatesAreConvertedToUnitRates() {
        var r = new RubroObra();
        RubroPlanning.apply(r,row(1L,null,null));
        assertEquals(1. / 155.4,r.getRendimientoOficial(),1e-8);
        assertEquals(2. / 155.4,r.getRendimientoAyudante(),1e-8);
    }
    private ProyectoCreateDto projectDto(List<RubroCreateDto> rows) {
        return new ProyectoCreateDto("Test",null,null,"Address","P1",null,null,null,null,null,null,null,null,null,List.of(),rows);
    }
    @Test void updateRetainsIdentityAndRejectsForeignIdsAndRemovalOfLinkedActivity() {
        var projects = mock(ProyectoRepository.class);
        var tasks = mock(ObraTareaRepository.class);
        var service = new UpdateProyectoUseCase(projects,mock(EmpresaRepository.class),mock(CatalogoEmpresaRepository.class),tasks,mock(com.vortise.gestion.infrastructure.security.CurrentTenantService.class));
        var p = new Proyecto(); p.setId(1L);
        var r = new RubroObra(); r.setId(10L); r.setProyecto(p);
        p.setRubros(new ArrayList<>(List.of(r)));
        when(projects.findById(1L)).thenReturn(Optional.of(p));
        when(projects.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tasks.findByProyectoId(1L)).thenReturn(List.of());
        var saved = service.execute(1L,projectDto(List.of(row(10L,10.,5.))));
        assertSame(r,p.getRubros().get(0)); assertEquals(10L,saved.rubros().get(0).id());
        assertThrows(IllegalArgumentException.class,() -> service.execute(1L,projectDto(List.of(row(99L,10.,5.)))));
        var t = new ObraTarea(); t.setActividad(r);
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(t));
        assertThrows(IllegalArgumentException.class,() -> service.execute(1L,projectDto(List.of())));
        assertSame(r,p.getRubros().get(0));
    }
}
