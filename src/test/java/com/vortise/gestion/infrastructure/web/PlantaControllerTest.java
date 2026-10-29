package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.PlantaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Direct controller tests: repository mocks only, no Spring context or database.
// HTTP exception mapping, bean validation and tenant policy need separate tests.
class PlantaControllerTest {
    private static final Long PLANTA_ID = 1001L;
    private static final Long PROJECT_ID = 2001L;

    private PlantaRepository plantas;
    private ProyectoRepository proyectos;
    private PlantaController controller;

    @BeforeEach
    void setUp() {
        plantas = mock(PlantaRepository.class);
        proyectos = mock(ProyectoRepository.class);
        var identity = mock(com.vortise.gestion.application.port.CurrentIdentity.class);
        when(proyectos.bloquearPorId(any())).thenAnswer(invocation -> {
            Empresa empresa = new Empresa("Own", "TEST-OWN");
            org.springframework.test.util.ReflectionTestUtils.setField(empresa, "id", 1L);
            return Optional.of(project(invocation.getArgument(0), empresa));
        });
        controller = new PlantaController(new com.vortise.gestion.application.usecase.PlantaUseCase(plantas, proyectos, identity));
    }

    @Test
    void updateOwnedPlantaPreservesIdentityAndPersistsSupportedFields() {
        Proyecto parent = project(PROJECT_ID, new Empresa("A", "TEST-A"));
        Planta planta = plant(parent);
        when(plantas.findById(PLANTA_ID)).thenReturn(Optional.of(planta));
        when(plantas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PlantaDto body = body(null, null);

        var response = controller.update(PROJECT_ID, PLANTA_ID, body);

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<Planta> saved = ArgumentCaptor.forClass(Planta.class);
        InOrder order = inOrder(plantas);
        order.verify(plantas).findById(PLANTA_ID);
        order.verify(plantas).save(saved.capture());
        assertSame(planta, saved.getValue());
        assertSame(parent, planta.getProyecto());
        PlantaDto expected = new PlantaDto(PLANTA_ID, PROJECT_ID, "P2", 200.0, 120.0, 80.0, 160.0, 0.8);
        assertEquals(expected, snapshot(planta));
        assertEquals(expected, response.getBody());
        verify(plantas, never()).deleteById(any());
        verifyNoMoreInteractions(plantas);
        verify(proyectos).bloquearPorId(any());
    }

    @Test
    void deleteOwnedPlantaDeletesOnlyVerifiedId() {
        Planta planta = plant(project(PROJECT_ID, null));
        PlantaDto before = snapshot(planta);
        Proyecto parent = planta.getProyecto();
        when(plantas.findById(PLANTA_ID)).thenReturn(Optional.of(planta));

        var response = controller.delete(PROJECT_ID, PLANTA_ID);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        InOrder order = inOrder(plantas);
        order.verify(plantas).findById(PLANTA_ID);
        order.verify(plantas).deleteById(PLANTA_ID);
        verify(plantas, never()).save(any());
        verifyNoMoreInteractions(plantas);
        verify(proyectos).bloquearPorId(any());
        assertEquals(before, snapshot(planta));
        assertSame(parent, planta.getProyecto());
    }

    @ParameterizedTest
    @MethodSource("bodyIdentities")
    void updateIgnoresBodyIdentity(Long bodyId, Long bodyProjectId) {
        Proyecto parent = project(PROJECT_ID, null);
        Planta planta = plant(parent);
        when(plantas.findById(PLANTA_ID)).thenReturn(Optional.of(planta));
        when(plantas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.update(PROJECT_ID, PLANTA_ID, body(bodyId, bodyProjectId));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(PLANTA_ID, planta.getId());
        assertSame(parent, planta.getProyecto());
        assertNotNull(response.getBody());
        assertEquals(PLANTA_ID, response.getBody().id());
        assertEquals(PROJECT_ID, response.getBody().proyectoId());
        verify(plantas).findById(PLANTA_ID);
        verify(plantas).save(same(planta));
        verifyNoMoreInteractions(plantas);
        verify(proyectos).bloquearPorId(any());
    }

    static Stream<Arguments> bodyIdentities() {
        return Stream.of(Arguments.of(null, null), Arguments.of(9001L, null),
            Arguments.of(null, 9002L), Arguments.of(9001L, 9002L));
    }

    @Test
    void updatePreservesNullableNumericFieldSemantics() {
        Proyecto parent = project(PROJECT_ID, null);
        Planta planta = plant(parent);
        when(plantas.findById(PLANTA_ID)).thenReturn(Optional.of(planta));
        when(plantas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.update(PROJECT_ID, PLANTA_ID,
            new PlantaDto(null, null, "P3", null, null, null, null, null));

        PlantaDto expected = new PlantaDto(PLANTA_ID, PROJECT_ID, "P3", null, null, null, null, null);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, snapshot(planta));
        assertEquals(expected, response.getBody());
        assertSame(parent, planta.getProyecto());
        verify(plantas).findById(PLANTA_ID);
        verify(plantas).save(same(planta));
        verifyNoMoreInteractions(plantas);
        verify(proyectos).bloquearPorId(any());
    }

    enum Denial { MISSING, SAME_COMPANY_WRONG_PROJECT, OTHER_COMPANY_WRONG_PROJECT, NULL_PARENT, NULL_PARENT_ID }

    @ParameterizedTest
    @EnumSource(Denial.class)
    void updateRejectsMissingOrMismatchedPlanta(Denial denial) {
        assertDenied(denial, true, body(null, null));
    }

    @ParameterizedTest
    @EnumSource(Denial.class)
    void deleteRejectsMissingOrMismatchedPlanta(Denial denial) {
        assertDenied(denial, false, null);
    }

    @ParameterizedTest
    @MethodSource("forgedIdentities")
    void forgedBodyIdentityCannotRescueWrongUrlParent(Long bodyId, Long bodyProjectId) {
        assertDenied(Denial.SAME_COMPANY_WRONG_PROJECT, true, body(bodyId, bodyProjectId));
    }

    static Stream<Arguments> forgedIdentities() {
        return Stream.of(Arguments.of(null, null), Arguments.of(9001L, PROJECT_ID),
            Arguments.of(PLANTA_ID, 2002L), Arguments.of(9001L, 2002L));
    }

    private void assertDenied(Denial denial, boolean update, PlantaDto body) {
        Empresa company = new Empresa("A", "TEST-A");
        Proyecto urlProject = project(PROJECT_ID, company);
        Planta planta = switch (denial) {
            case MISSING -> null;
            case SAME_COMPANY_WRONG_PROJECT -> plant(project(2002L, company));
            case OTHER_COMPANY_WRONG_PROJECT -> plant(project(2002L, new Empresa("B", "TEST-B")));
            case NULL_PARENT -> plant(null);
            case NULL_PARENT_ID -> plant(project(null, company));
        };
        PlantaDto before = planta == null ? null : snapshot(planta);
        Proyecto parent = planta == null ? null : planta.getProyecto();
        when(plantas.findById(PLANTA_ID)).thenReturn(Optional.ofNullable(planta));

        com.vortise.gestion.application.exception.RecursoNoEncontradoException error = assertThrows(com.vortise.gestion.application.exception.RecursoNoEncontradoException.class, () -> {
            if (update) {
                controller.update(urlProject.getId(), PLANTA_ID, body);
            } else {
                controller.delete(urlProject.getId(), PLANTA_ID);
            }
        });

        assertEquals("Planta no encontrada", error.getMessage());
        verify(plantas).findById(PLANTA_ID);
        verify(plantas, never()).save(any());
        verify(plantas, never()).deleteById(any());
        verifyNoMoreInteractions(plantas);
        verify(proyectos).bloquearPorId(any());
        if (planta != null) {
            assertEquals(before, snapshot(planta));
            assertSame(parent, planta.getProyecto());
        }
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    @SuppressWarnings("removal") // Intentionally distinct boxes to detect reference comparison.
    void comparesLongProjectIdsByValue(Operation operation) {
        Long urlId = new Long(2001L);
        Long persistedId = new Long(2001L);
        assertNotSame(urlId, persistedId);
        assertEquals(urlId, persistedId);
        Planta planta = plant(project(persistedId, null));
        when(plantas.findById(PLANTA_ID)).thenReturn(Optional.of(planta));

        if (operation == Operation.PUT) {
            when(plantas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertEquals(200, controller.update(urlId, PLANTA_ID, body(null, null)).getStatusCode().value());
            verify(plantas).save(same(planta));
        } else {
            assertEquals(204, controller.delete(urlId, PLANTA_ID).getStatusCode().value());
            verify(plantas).deleteById(PLANTA_ID);
        }
        verify(plantas).findById(PLANTA_ID);
        verifyNoMoreInteractions(plantas);
        verify(proyectos).bloquearPorId(any());
    }

    enum Operation { PUT, DELETE }

    private static Proyecto project(Long id, Empresa company) {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(id);
        proyecto.setEmpresaRelacionada(company);
        return proyecto;
    }

    private static Planta plant(Proyecto parent) {
        return new Planta(PLANTA_ID, parent, "PB", 100.0, 60.0, 40.0, 80.0, 0.5);
    }

    private static PlantaDto body(Long id, Long projectId) {
        return new PlantaDto(id, projectId, "P2", 200.0, 120.0, 80.0, 160.0, 0.8);
    }

    private static PlantaDto snapshot(Planta planta) {
        Long projectId = planta.getProyecto() == null ? null : planta.getProyecto().getId();
        return new PlantaDto(planta.getId(), projectId, planta.getNivel(), planta.getSuperficie(),
            planta.getSupCubierta(), planta.getSupDescubierta(), planta.getSupCalculada(), planta.getCoefPlantaTipo());
    }
}
