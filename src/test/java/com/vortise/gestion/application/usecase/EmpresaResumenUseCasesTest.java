package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmpresaResumenUseCasesTest {
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final RestriccionObraRepository restricciones = mock(RestriccionObraRepository.class);
    private final RegistroHorasRepository horas = mock(RegistroHorasRepository.class);
    private final ObraTareaRepository tareas = mock(ObraTareaRepository.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final GetEmpresaResumenUseCase resumen = new GetEmpresaResumenUseCase(empresas, proyectos, restricciones, horas, tareas, identity);
    private final GetRestriccionesEmpresaUseCase listado = new GetRestriccionesEmpresaUseCase(empresas, restricciones, identity);

    private static EmpresaConteoProjection row(long empresaId, long cantidad) {
        return new EmpresaConteoProjection() {
            public Long getEmpresaId() { return empresaId; }
            public long getCantidad() { return cantidad; }
        };
    }

    private void companyExists() { when(empresas.findById(1L)).thenReturn(Optional.of(new Empresa("Sintética", null))); }

    /** Activity of 100 units budgeted at 500 hours: 5 h/unit; 50 hours worked plan 10 units. */
    private final Proyecto project = new Proyecto();
    private final RubroObra activity = new RubroObra();
    {
        project.setId(5L);
        activity.setId(10L); activity.setProyecto(project); activity.setCantidad(100.);
        activity.setHorasOficialesPrevistas(400.); activity.setHorasAyudantesPrevistas(100.);
        project.setRubros(List.of(activity));
    }

    private ObraTarea task(long id, Double realQuantity, Double persistedPercentage) {
        var task = new ObraTarea();
        task.setId(id); task.setProyecto(project); task.setActividad(activity);
        task.setHorasOficiales(40.); task.setHorasAyudantes(10.);
        task.setCantidadReal(realQuantity); task.setPorcentajeCumplimiento(persistedPercentage);
        return task;
    }

    @Test
    void summarizesCompanyTotals() {
        companyExists();
        when(proyectos.contarPorEmpresaRelacionada(Set.of(1L))).thenReturn(List.of(row(1L, 4)));
        when(restricciones.contarPorEmpresaRelacionada(Set.of(1L))).thenReturn(List.of(row(1L, 37)));
        when(horas.sumHorasPorEmpresaRelacionada(1L)).thenReturn(812.5);
        when(tareas.findConActividadByEmpresaRelacionadaId(1L)).thenReturn(List.of(task(1, 10., null), task(2, 5., null), task(3, 12., null)));
        var result = resumen.execute(1L);
        assertEquals(1L, result.empresaId());
        assertEquals(4, result.obras());
        assertEquals(37, result.restricciones());
        assertEquals(812.5, result.horasRegistradas());
        assertEquals(3, result.cargasVinculadas());
        assertEquals(2, result.cargasCumplidas(), "10 of 10 and 12 of 10 units comply; 5 of 10 does not");
        verify(identity).requireEmpresa(1L);
    }

    @Test
    void completionUsesTheDerivedValueShownToUsersNotTheStoredColumn() {
        companyExists();
        when(proyectos.contarPorEmpresaRelacionada(any())).thenReturn(List.of());
        when(restricciones.contarPorEmpresaRelacionada(any())).thenReturn(List.of());
        when(tareas.findConActividadByEmpresaRelacionadaId(1L)).thenReturn(List.of(task(1, 5., 100.), task(2, 10., 0.)));
        var result = resumen.execute(1L);
        assertEquals(1, result.cargasCumplidas(), "stored 100% with half the real quantity is not complete; stored 0% with full quantity is");
        assertEquals(CreateObraTareaUseCase.toDto(task(2, 10., 0.)).porcentajeCumplimiento() >= 100.0, true);
    }

    @Test
    void companyWithoutDataReturnsZeros() {
        companyExists();
        when(proyectos.contarPorEmpresaRelacionada(Set.of(1L))).thenReturn(List.of());
        when(restricciones.contarPorEmpresaRelacionada(Set.of(1L))).thenReturn(List.of());
        when(horas.sumHorasPorEmpresaRelacionada(1L)).thenReturn(null);
        when(tareas.findConActividadByEmpresaRelacionadaId(1L)).thenReturn(List.of());
        var result = resumen.execute(1L);
        assertEquals(0, result.obras() + result.restricciones() + result.cargasVinculadas() + result.cargasCumplidas());
        assertEquals(0.0, result.horasRegistradas());
    }

    @Test
    void deniedCompanyReadsNothing() {
        doThrow(new AccessDeniedException("No tienes acceso a esta empresa")).when(identity).requireEmpresa(2L);
        assertThrows(AccessDeniedException.class, () -> resumen.execute(2L));
        assertThrows(AccessDeniedException.class, () -> listado.execute(2L));
        verifyNoInteractions(empresas, proyectos, restricciones, horas, tareas);
    }

    @Test
    void missingAccessibleCompanyIsNotFound() {
        when(empresas.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> resumen.execute(1L));
        assertThrows(RecursoNoEncontradoException.class, () -> listado.execute(1L));
        verifyNoInteractions(proyectos, restricciones, horas, tareas);
    }

    @Test
    void listsEveryRestrictionOfTheCompanyWithTheSameDtoAsTheObraEndpoint() {
        companyExists();
        var first = restriction(11L, 5L, "Obra A", "Planos"); var second = restriction(12L, 6L, "Obra B", "Acopio");
        when(restricciones.findByProyectoEmpresaRelacionadaIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));
        var result = listado.execute(1L);
        assertEquals(List.of(11L, 12L), result.stream().map(item -> item.id()).toList());
        assertEquals(List.of(5L, 6L), result.stream().map(item -> item.proyectoId()).toList());
        assertEquals(List.of("Obra A", "Obra B"), result.stream().map(item -> item.proyectoNombre()).toList());
        assertEquals(RestriccionMapper.toDto(first), result.get(0));
        verify(identity).requireEmpresa(1L);
    }

    private static RestriccionObra restriction(long id, long projectId, String projectName, String text) {
        var project = new Proyecto(); project.setId(projectId); project.setNombre(projectName);
        var item = new RestriccionObra(); item.setId(id); item.setProyecto(project); item.setRestriccion(text);
        return item;
    }
}
