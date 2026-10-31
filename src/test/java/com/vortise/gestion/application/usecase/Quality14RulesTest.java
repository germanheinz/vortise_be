package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.application.exception.*;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Pure rules and mock repositories only; never starts Spring or a datasource. */
class Quality14RulesTest {
    @ParameterizedTest @NullSource @ValueSource(strings = {"", "a", "1234567", "        "})
    void rejectsShortOrMissingPasswords(String value) {
        assertThrows(SolicitudInvalidaException.class, () -> PasswordPolicy.validate(value));
    }
    @Test void enforcesUtf8ByteBoundaryAndAcceptsValidPasswords() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("12345678"));
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(72)));
        assertDoesNotThrow(() -> PasswordPolicy.validate("á".repeat(36)));
        assertThrows(SolicitudInvalidaException.class, () -> PasswordPolicy.validate("á".repeat(37)));
        assertThrows(SolicitudInvalidaException.class, () -> PasswordPolicy.validate("a".repeat(73)));
    }
    @Test void bothHourInputsPreserveFirstMatchingNullProductivity() {
        Proyecto project = new Proyecto();
        RubroObra first = new RubroObra(); first.setRubro("Trabajo"); first.setProductividad(null);
        RubroObra second = new RubroObra(); second.setRubro("Trabajo"); second.setProductividad(5.0);
        project.setRubros(new ArrayList<>(List.of(first, second)));
        OffsetDateTime start = OffsetDateTime.parse("2026-10-29T08:00:00Z");
        RegistroHorasDatos create = new RegistroHorasCreateDto(null, "Trabajo", null, null, null, start,
            start.plusHours(1), null, null, null, null, null, null, null, null);
        RegistroHorasDatos update = new RegistroHorasUpdateDto(null, "Trabajo", null, null, null, start,
            start.plusHours(1), null, null, null, null, null, null, null, null);
        for (var input : List.of(create, update)) {
            RegistroHoras record = new RegistroHoras();
            assertDoesNotThrow(() -> RegistroHorasRules.apply(input, record, project));
            assertNull(record.getProductividadPresupuesto());
            assertEquals(1.0, record.getHoras());
            first.setProductividad(2.0);
            RegistroHorasRules.apply(input, record, project);
            assertEquals(2.0, record.getProductividadPresupuesto());
            first.setProductividad(null);
        }
        project.setRubros(List.of());
        RegistroHoras record = new RegistroHoras();
        RegistroHorasRules.apply(create, record, project);
        assertNull(record.getProductividadPresupuesto());
    }
    @Test void cyclicEntityGraphHasStableHashAndBoundedDiagnostics() {
        Proyecto project = new Proyecto();
        Planta plant = new Planta(); plant.setProyecto(project); project.getPlantas().add(plant);
        int before = assertDoesNotThrow(project::hashCode);
        assertDoesNotThrow(plant::hashCode);
        assertTrue(project.toString().length() < 100);
        assertTrue(plant.toString().length() < 100);
        assertNotEquals(project, new Proyecto());
        project.setId(7L);
        assertEquals(before, project.hashCode());
        Proyecto sameIdentity = new Proyecto(); sameIdentity.setId(7L); sameIdentity.setNombre("Different data");
        assertEquals(project, sameIdentity);
    }
    @Test void omittedCollectionsPreserveLinkedActivitiesAndPlants() {
        ProyectoRepository projects = mock(ProyectoRepository.class);
        var restrictions = mock(RestriccionObraRepository.class);
        var hours = mock(RegistroHorasRepository.class);
        var tasks = mock(ObraTareaRepository.class);
        var identity = mock(CurrentIdentity.class);
        Empresa company = new Empresa("A", null); ReflectionTestUtils.setField(company, "id", 1L);
        Proyecto project = new Proyecto(); project.setId(7L); project.setEmpresaRelacionada(company);
        RubroObra activity = new RubroObra(); activity.setId(8L); activity.setSubRubro("Activity");
        activity.setProyecto(project); project.getRubros().add(activity);
        Planta plant = new Planta(); plant.setProyecto(project); project.getPlantas().add(plant);
        when(projects.bloquearPorId(7L)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        when(restrictions.countByRubroVinculadoId(8L)).thenReturn(1L);
        var changes = mock(AplicarCambiosEmpresaObraUseCase.class);
        when(changes.execute(any(), any(), any())).thenReturn(List.of());
        var useCase = new UpdateProyectoUseCase(projects, mock(EmpresaRepository.class), mock(CatalogoEmpresaRepository.class), tasks, restrictions, identity, hours, changes);
        var input = new ProyectoUpdateDto("Name", null, null, "Address", "P1", null, null, null, null,
            null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> useCase.execute(7L, input));
        assertSame(activity, project.getRubros().get(0));
        assertSame(plant, project.getPlantas().get(0));
        verify(identity).requireEmpresa(1L);
    }
}
