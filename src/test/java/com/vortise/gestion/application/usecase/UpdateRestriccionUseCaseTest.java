package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateRestriccionUseCaseTest {
    private RestriccionObraRepository repository;
    private RubroObraRepository rubros;
    private UpdateRestriccionUseCase useCase;
    private RestriccionObra restriction;
    private Proyecto project;
    private final LocalDate start = LocalDate.of(2026, 10, 1);
    private final LocalDate due = LocalDate.of(2026, 10, 10);

    @BeforeEach void setup() {
        repository = mock(RestriccionObraRepository.class);
        rubros = mock(RubroObraRepository.class);
        useCase = new UpdateRestriccionUseCase(repository, rubros);
        project = new Proyecto(); project.setId(1L); project.setNombre("Obra");
        restriction = new RestriccionObra(); restriction.setId(2L); restriction.setProyecto(project);
        restriction.setArea("Compras"); restriction.setResponsable("Ana");
        restriction.setFechaSolicitud(start); restriction.setFechaMaximaEntrega(due);
        when(repository.findById(2L)).thenReturn(Optional.of(restriction));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void boardStatusUpdatePreservesAreaAndDates() {
        var result = useCase.execute(2L, new RestriccionUpdateDto(null, "RESUELTO", 3));
        assertEquals("Compras", result.area()); assertEquals("Ana", result.responsable());
        assertEquals(start, result.fechaSolicitud()); assertEquals(due, result.fechaMaximaEntrega());
        assertNull(result.fechaRealEntrega()); assertEquals("RESUELTO", result.estado());
    }

    @Test void linksPreviouslyUnassignedRestrictionToSubrubroFromSameProject() {
        RubroObra activity = new RubroObra();
        activity.setId(31L); activity.setProyecto(project); activity.setRubro("Instalaciones");
        activity.setSubRubro("Tendido eléctrico"); activity.setNivel("Nivel 2"); activity.setUnidad("m");
        when(rubros.findById(31L)).thenReturn(Optional.of(activity));

        var result = useCase.execute(2L, new RestriccionUpdateDto(null, null, null,
            null, null, null, null, null, false, 31L));

        assertEquals(31L, result.rubroId());
        assertEquals("Instalaciones · Tendido eléctrico", result.rubro());
        assertSame(activity, restriction.getRubroVinculado());
    }

    @Test void rejectsLinkToSubrubroFromAnotherProject() {
        Proyecto otherProject = new Proyecto(); otherProject.setId(3L);
        RubroObra foreignActivity = new RubroObra();
        foreignActivity.setId(31L); foreignActivity.setProyecto(otherProject); foreignActivity.setSubRubro("Tendido eléctrico");
        when(rubros.findById(31L)).thenReturn(Optional.of(foreignActivity));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(2L,
            new RestriccionUpdateDto(null, null, null, null, null, null, null, null, false, 31L)));
        assertNull(restriction.getRubroVinculado());
        verify(repository, never()).save(any());
    }

    @Test void storesIndependentAreaAndDeliveryDatesAndSupportsExplicitClearing() {
        var real = due.plusDays(2);
        var result = useCase.execute(2L, new RestriccionUpdateDto(null, null, null,
            " Ingeniería ", "Luis", start, due, real, true));
        assertEquals("Ingeniería", result.area()); assertEquals("Luis", result.responsable());
        assertEquals(real, result.fechaRealEntrega());
        var cleared = useCase.execute(2L, new RestriccionUpdateDto(null, null, null,
            "", "", start, due, null, true));
        assertNull(cleared.area()); assertNull(cleared.responsable()); assertNull(cleared.fechaRealEntrega());
        assertEquals(start, cleared.fechaSolicitud()); assertEquals(due, cleared.fechaMaximaEntrega());
    }

    @Test void rejectsInvalidDatesBeforeMutatingOrSaving() {
        for (var dates : new LocalDate[][]{{start, start.minusDays(1), null}, {start, due, start.minusDays(1)}}) {
            assertThrows(IllegalArgumentException.class, () -> useCase.execute(2L,
                new RestriccionUpdateDto(null, null, null, "Nueva", null, dates[0], dates[1], dates[2], true)));
        }
        assertEquals("Compras", restriction.getArea()); assertEquals(due, restriction.getFechaMaximaEntrega());
        verify(repository, never()).save(any());
    }

    @Test void areaLengthAndNormalizationAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(2L,
            new RestriccionUpdateDto(null, null, null, "x".repeat(121), null, null, null, null, false)));
        assertEquals("Compras", restriction.getArea()); verify(repository, never()).save(any());
    }
}