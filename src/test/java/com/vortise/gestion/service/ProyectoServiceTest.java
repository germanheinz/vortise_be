package com.vortise.gestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProyectoServiceTest {
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final ProyectoService service = new ProyectoService(proyectos, null, null, null, null);

    @Test
    void eliminaUnProyectoExistente() {
        when(proyectos.findById(7L)).thenReturn(Optional.of(new Proyecto()));

        service.eliminar(7L);

        verify(proyectos).deleteById(7L);
    }

    @Test
    void conservaElErrorCuandoElProyectoNoExiste() {
        when(proyectos.findById(7L)).thenReturn(Optional.empty());

        var error = assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(7L));

        assertEquals("Proyecto no encontrado", error.getMessage());
        verify(proyectos, never()).deleteById(7L);
    }
}
