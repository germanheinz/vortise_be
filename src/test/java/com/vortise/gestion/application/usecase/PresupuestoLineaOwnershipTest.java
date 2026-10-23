package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.PresupuestoLineaDto;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresupuestoLineaOwnershipTest {
    @Mock private PresupuestoLineaRepository lineas;
    @Mock private ProyectoRepository proyectos;
    @Mock private Proyecto requestedProject;
    @Mock private Proyecto foreignProject;
    @Mock private PresupuestoLinea foreignLine;

    @Test
    void updateRejectsLineThatBelongsToAnotherProjectWithoutSaving() {
        when(requestedProject.getId()).thenReturn(10L);
        when(foreignProject.getId()).thenReturn(20L);
        when(proyectos.findById(10L)).thenReturn(Optional.of(requestedProject));
        when(lineas.findById(99L)).thenReturn(Optional.of(foreignLine));
        when(foreignLine.getProyecto()).thenReturn(foreignProject);

        SavePresupuestoLineaUseCase useCase = new SavePresupuestoLineaUseCase(lineas, proyectos);
        PresupuestoLineaDto request = new PresupuestoLineaDto(99L, 10L, "Etapa", "Cubierta", "Vivienda",
            null, 10.0, 2.0, 3.0, null, 1);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
        verify(lineas, never()).save(foreignLine);
        verify(foreignLine, never()).setEtapa(org.mockito.ArgumentMatchers.anyString());
        verify(foreignLine, never()).setProyecto(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRejectsLineThatBelongsToAnotherProjectWithoutDeleting() {
        when(foreignProject.getId()).thenReturn(20L);
        when(lineas.findById(99L)).thenReturn(Optional.of(foreignLine));
        when(foreignLine.getProyecto()).thenReturn(foreignProject);

        DeletePresupuestoLineaUseCase useCase = new DeletePresupuestoLineaUseCase(lineas);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 99L));
        verify(lineas, never()).deleteById(99L);
    }
}
