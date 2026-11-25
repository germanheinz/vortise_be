package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import static org.mockito.Mockito.*;

class PrevistoRealUseCaseTest {
    private final ProyectoRepository projects = mock(ProyectoRepository.class);
    private final ObraTareaRepository tasks = mock(ObraTareaRepository.class);
    private final CategoriaManoObraEmpresaRepository categories = mock(CategoriaManoObraEmpresaRepository.class);
    private final CalendarioLaboralEmpresaRepository calendars = mock(CalendarioLaboralEmpresaRepository.class);
    private final Proyecto project = new Proyecto();
    private final RubroObra activity = new RubroObra();
    PrevistoRealUseCaseTest() {
        project.setId(1L);
        activity.setId(10L); activity.setProyecto(project); activity.setCantidad(155.4);
        activity.setHorasOficialesPrevistas(4000.); activity.setHorasAyudantesPrevistas(1881.5);
        activity.setRubro("Hormigón"); activity.setUnidad("m3");
        project.setRubros(List.of(activity));
        when(projects.findById(1L)).thenReturn(Optional.of(project));
    }
    private ObraTarea task(long id, Long activityId, double quantity) {
        var t = new ObraTarea(); t.setId(id); t.setProyecto(project); if (activityId != null) { var r = new RubroObra(); r.setId(activityId); t.setActividad(r); }
        t.setCantidadReal(quantity); t.setHorasOficiales(100.); t.setHorasAyudantes(80.); return t;
    }
    @Test void aggregateUsesOnlyLinkedActivityAndRecomputesAfterEditAndDelete() {
        var a = task(1,10L,6); var b = task(2,10L,5);
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(a,b,task(3,null,100),task(4,99L,100)));
        var service = new GetPrevistoRealUseCase(projects,tasks,categories,calendars);
        var result = service.execute(1L);
        assertEquals(2,result.tareasSinVincular());
        assertEquals(11.,result.actividades().get(0).cantidadReal());
        assertEquals(1.156451094,result.actividades().get(0).indicadores().productividad(),1e-8);
        b.setCantidadReal(6.);
        assertEquals(12.,service.execute(1L).actividades().get(0).cantidadReal());
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(a));
        assertEquals(6.,service.execute(1L).actividades().get(0).cantidadReal());
        verify(tasks,never()).save(any());
    }
    @Test void annexBasesUseWorkedDaysAndRecomputeAfterEditsOrDeletion() {
        var company = mock(Empresa.class); when(company.getId()).thenReturn(20L); project.setEmpresaRelacionada(company);
        var calendario = mock(CalendarioLaboralEmpresa.class); when(calendario.getHorasLunesAViernes()).thenReturn(8.);
        when(calendars.findByEmpresaId(20L)).thenReturn(Optional.of(calendario));
        var root = new RubroObra(); root.setId(9L); root.setTipoNodo(TipoNodoRubro.RUBRO);
        root.setCantidadPersonas(2);
        activity.setParent(root); activity.setCantidad(20.);
        activity.setHorasOficialesPrevistas(50.); activity.setHorasAyudantesPrevistas(50.);
        // Configurar fecha de inicio planificada para que el cálculo de días transcurridos funcione
        activity.setFechaInicioPlanificada(LocalDate.now().minusDays(2));
        project.setRubros(List.of(root, activity));
        var primero = task(1, 10L, 6);
        primero.setDias(2); primero.setHorasOficiales(25.); primero.setHorasAyudantes(25.);
        var segundo = task(2, 10L, 4);
        segundo.setDias(1); segundo.setHorasOficiales(15.); segundo.setHorasAyudantes(15.);
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(primero, segundo));
        var service = new GetPrevistoRealUseCase(projects, tasks, categories, calendars);

        var cargas = service.execute(1L).actividades().get(0).cargas();
        assertEquals(new GetPrevistoRealUseCase.BaseAnexo(2, 2, 32., 30., 50.), cargas.get(0).baseAnexo());
        assertEquals(new GetPrevistoRealUseCase.BaseAnexo(1, 2, 16., 20., 30.), cargas.get(1).baseAnexo());
        segundo.setDias(0); segundo.setCantidadReal(0.);
        assertEquals(new GetPrevistoRealUseCase.BaseAnexo(0, 2, 0., 0., 30.),
            service.execute(1L).actividades().get(0).cargas().get(1).baseAnexo());
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(primero));
        assertEquals(1, service.execute(1L).actividades().get(0).cargas().size());
    }

    private ObraTareaCreateDto dto(Long rubroId) {
        return new ObraTareaCreateDto("Carga",null,null,"kg","wrong",null,null,null,null,100.,80.,null,6.,null,null,LocalDate.of(2026,10,14),rubroId);
    }
    @Test void savesAnActivityInProcessWithoutExecutedQuantity() {
        when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        var sinCantidad = new ObraTareaCreateDto("Carga",null,null,"kg","x",null,null,null,null,100.,80.,null,null,null,null,LocalDate.of(2026,10,14),10L);
        var saved = new CreateObraTareaUseCase(tasks,projects).execute(1L,sinCantidad);
        assertEquals(10L,saved.rubroId()); assertNull(saved.cantidadReal()); assertNull(saved.porcentajeCumplimiento());
        var invalida = new ObraTareaCreateDto("Carga",null,null,"kg","x",null,null,null,null,100.,80.,null,-1.,null,null,LocalDate.of(2026,10,14),10L);
        assertThrows(SolicitudInvalidaException.class,() -> new CreateObraTareaUseCase(tasks,projects).execute(1L,invalida));
    }
    @Test void loadsWithoutQuantityAddNothingButDoNotErasePhysicalProgress() {
        var enProceso = task(2,10L,0); enProceso.setCantidadReal(null);
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(task(1,10L,6),enProceso));
        var service = new GetPrevistoRealUseCase(projects,tasks,categories,calendars);
        assertEquals(6.,service.execute(1L).actividades().get(0).cantidadReal());
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(enProceso));
        assertNull(service.execute(1L).actividades().get(0).cantidadReal());
    }
    @Test void desencofradoLoadsOnlyImputeHoursAndAreFlaggedOnTheActivity() {
        when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new CreateObraTareaUseCase(tasks,projects);
        var soloHoras = new ObraTareaCreateDto("Desencofrado",null,null,"kg","x",null,null,null,null,40.,40.,null,null,null,null,LocalDate.of(2026,10,14),10L,1,true);
        var saved = service.execute(1L,soloHoras);
        assertTrue(saved.desencofrado()); assertNull(saved.cantidadReal());
        var conCantidad = new ObraTareaCreateDto("Desencofrado",null,null,"kg","x",null,null,null,null,40.,40.,null,15.,null,null,LocalDate.of(2026,10,14),10L,1,true);
        assertThrows(SolicitudInvalidaException.class,() -> service.execute(1L,conCantidad));
        var normal = task(1,10L,6); var desencofrado = task(2,10L,0); desencofrado.setCantidadReal(null); desencofrado.setDesencofrado(true);
        var summary = new GetPrevistoRealUseCase(projects,tasks,categories,calendars);
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(normal));
        assertFalse(summary.execute(1L).actividades().get(0).conDesencofrado());
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(normal,desencofrado));
        var activityResult = summary.execute(1L).actividades().get(0);
        assertTrue(activityResult.conDesencofrado());
        assertEquals(6.,activityResult.cantidadReal(),"the desencofrado hours add no quantity");
        assertEquals(360.,activityResult.horasOficiales()+activityResult.horasAyudantes(),"but their hours do count: 180 HH per load, two loads");
    }
    private ObraTareaCreateDto conDias(Integer dias) {
        return new ObraTareaCreateDto("Carga",null,null,"kg","wrong",null,null,null,null,100.,80.,null,6.,null,null,LocalDate.of(2026,10,14),10L,dias);
    }
    @Test void loadSpansTheRequestedDaysAndDefaultsToOneWhenOmitted() {
        when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new CreateObraTareaUseCase(tasks,projects);
        assertEquals(3,service.execute(1L,conDias(3)).dias());
        assertEquals(1,service.execute(1L,dto(10L)).dias(),"a client that does not send days gets one day");
    }
    @Test void rejectsDaysOutOfRangeAndKeepsThemWhenAnEditOmitsThem() {
        when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new CreateObraTareaUseCase(tasks,projects);
        assertThrows(SolicitudInvalidaException.class,() -> service.execute(1L,conDias(0)));
        assertThrows(SolicitudInvalidaException.class,() -> service.execute(1L,conDias(ObraTareaCreateDto.MAX_DIAS + 1)));
        var existing = task(1,10L,6); existing.setDias(4);
        when(tasks.findById(1L)).thenReturn(Optional.of(existing));
        assertEquals(4,service.update(1L,1L,dto(10L)).dias(),"an edit without days does not reset them");
        assertEquals(2,service.update(1L,1L,conDias(2)).dias());
    }
    @Test void rejectsForeignActivityBeforeSaving() {
        assertThrows(IllegalArgumentException.class,() -> new CreateObraTareaUseCase(tasks,projects).execute(1L,dto(99L)));
        verify(tasks,never()).save(any());
    }
    @Test void derivesIdentityFromOwnedActivity() {
        when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        var saved = new CreateObraTareaUseCase(tasks,projects).execute(1L,dto(10L));
        assertEquals(10L,saved.rubroId()); assertEquals("m3",saved.unidad()); assertEquals("Hormigón",saved.rubro());
    }
    @Test void rejectsUpdatingTaskFromAnotherProject() {
        var other = new Proyecto(); other.setId(2L);
        var t = task(1,10L,6); t.setProyecto(other);
        when(tasks.findById(1L)).thenReturn(Optional.of(t));
        assertThrows(IllegalArgumentException.class,() -> new CreateObraTareaUseCase(tasks,projects).update(1L,1L,dto(10L)));
        verify(tasks,never()).save(any());
    }
    @Test void readsExistingCompanyRatesAndCalendarWithoutWriting() {
        var company = mock(Empresa.class); when(company.getId()).thenReturn(20L); project.setEmpresaRelacionada(company);
        when(categories.findByEmpresaId(20L)).thenReturn(List.of(new CategoriaManoObraEmpresa(company,"OFICIAL",2000.,true),
            new CategoriaManoObraEmpresa(company,"AYUDANTE",1000.,true)));
        when(calendars.findByEmpresaId(20L)).thenReturn(Optional.of(new CalendarioLaboralEmpresa(company)));
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(task(1,10L,6)));
        var result = new GetPrevistoRealUseCase(projects,tasks,categories,calendars).execute(1L);
        assertEquals(101525.096525,result.actividades().get(0).indicadores().desvio(),1e-5);
        assertEquals(8.,result.horasJornada());
        verify(categories,never()).save(any()); verify(calendars,never()).save(any());
    }
    @Test void unlinkedNewTaskIsRejected() {
        assertThrows(IllegalArgumentException.class,() -> new CreateObraTareaUseCase(tasks,projects).execute(1L,dto(null)));
        verify(tasks,never()).save(any());
    }
    @Test void subcontractedActivityAcceptsLoadsWithoutHoursAndDropsAnySent() {
        activity.setTipoContratista("SUBCONTRATO");
        when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        var withoutHours = new ObraTareaCreateDto("Carga",null,null,"kg","wrong",null,null,null,null,null,null,null,6.,null,null,LocalDate.of(2026,10,14),10L);
        var saved = new CreateObraTareaUseCase(tasks,projects).execute(1L,withoutHours);
        assertNull(saved.horasOficiales()); assertNull(saved.horasAyudantes()); assertEquals(6.,saved.cantidadReal());
        var ignored = new CreateObraTareaUseCase(tasks,projects).execute(1L,dto(10L));
        assertNull(ignored.horasOficiales()); assertNull(ignored.horasAyudantes());
        var withoutQuantity = new ObraTareaCreateDto("Carga",null,null,"kg","wrong",null,null,null,null,null,null,null,null,null,null,LocalDate.of(2026,10,14),10L);
        // La cantidad ejecutada es opcional: la carga queda como actividad en proceso.
        assertNull(new CreateObraTareaUseCase(tasks,projects).execute(1L,withoutQuantity).cantidadReal());
    }
    @Test void ownLaborActivityStillRequiresHours() {
        var withoutHours = new ObraTareaCreateDto("Carga",null,null,"kg","wrong",null,null,null,null,null,null,null,6.,null,null,LocalDate.of(2026,10,14),10L);
        assertThrows(IllegalArgumentException.class,() -> new CreateObraTareaUseCase(tasks,projects).execute(1L,withoutHours));
        verify(tasks,never()).save(any());
    }

}
