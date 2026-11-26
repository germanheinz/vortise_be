package com.vortise.gestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.exception.ReglaDeEstadoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RestriccionObra;
import com.vortise.gestion.repository.RestriccionObraRepository;
import com.vortise.gestion.repository.RubroObraRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.vortise.gestion.application.exception.OperacionNoPermitidaException;

class UpdateRestriccionJefeRulesTest {
    private final RestriccionObraRepository repository = mock(RestriccionObraRepository.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final RestriccionService useCase = new RestriccionService(repository, null, mock(RubroObraRepository.class), identity, null, null);
    private RestriccionObra restriction;

    @BeforeEach
    void setup() {
        var project = new Proyecto();
        project.setId(1L);
        restriction = new RestriccionObra();
        restriction.setId(2L);
        restriction.setProyecto(project);
        restriction.setEstado("PENDIENTE");
        restriction.setFechaSolicitud(LocalDate.of(2026, 10, 1));
        when(repository.findById(2L)).thenReturn(Optional.of(restriction));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(identity.isJefeDeObra()).thenReturn(true);
    }

    @Test
    void jefeCanReleaseAPendingRestriction() {
        assertEquals("RESUELTO", useCase.actualizar(2L, new RestriccionUpdateDto(null, "RESUELTO", 3)).estado());
    }

    @Test
    void jefeCannotChangeAResolvedRestrictionAgain() {
        restriction.setEstado("RESUELTO");

        assertThrows(ReglaDeEstadoException.class, () -> useCase.actualizar(2L, new RestriccionUpdateDto(null, "PENDIENTE", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void jefeCannotEditRestrictionData() {
        assertThrows(OperacionNoPermitidaException.class, () -> useCase.actualizar(2L, new RestriccionUpdateDto("ALTA", null, null)));
        assertThrows(OperacionNoPermitidaException.class, () -> useCase.actualizar(2L, new RestriccionUpdateDto("ALTA", "RESUELTO", null)));
        assertThrows(OperacionNoPermitidaException.class, () -> useCase.actualizar(2L, new RestriccionUpdateDto(null, null, null,
            "Compras", null, null, null, null, false, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void managersAreNotSubjectToTheRules() {
        when(identity.isJefeDeObra()).thenReturn(false);
        restriction.setEstado("RESUELTO");

        assertEquals("PENDIENTE", useCase.actualizar(2L, new RestriccionUpdateDto("ALTA", "PENDIENTE", null)).estado());
    }
}
