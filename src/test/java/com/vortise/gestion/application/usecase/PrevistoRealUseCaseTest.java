package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
    private ObraTareaCreateDto dto(Long rubroId) {
        return new ObraTareaCreateDto("Carga",null,null,"kg","wrong",null,null,null,null,100.,80.,null,6.,null,null,LocalDate.of(2026,10,14),rubroId);
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

}
