package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateRestriccionUseCaseTest {
    private RestriccionObraRepository restricciones;
    private ProyectoRepository proyectos;
    private RubroObraRepository rubros;
    private CreateRestriccionUseCase useCase;
    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        restricciones = mock(RestriccionObraRepository.class);
        proyectos = mock(ProyectoRepository.class);
        rubros = mock(RubroObraRepository.class);
        useCase = new CreateRestriccionUseCase(restricciones, proyectos, rubros);
        proyecto = new Proyecto();
        proyecto.setId(10L);
        proyecto.setNombre("Obra de prueba");
        when(proyectos.findById(10L)).thenReturn(Optional.of(proyecto));
    }

    @Test
    void acceptsSameProjectSubrubroAndDerivesActivityFields() {
        RubroObra activity = new RubroObra();
        activity.setId(31L);
        activity.setProyecto(proyecto);
        activity.setRubro("Instalaciones");
        activity.setSubRubro("Tendido eléctrico");
        activity.setNivel("Nivel 2");
        activity.setUnidad("m");
        when(rubros.findById(31L)).thenReturn(Optional.of(activity));
        when(restricciones.save(any(RestriccionObra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(10L, request(31L));

        ArgumentCaptor<RestriccionObra> saved = ArgumentCaptor.forClass(RestriccionObra.class);
        verify(restricciones).save(saved.capture());
        assertSame(activity, saved.getValue().getRubroVinculado());
        assertEquals(31L, result.rubroId());
        assertEquals("Instalaciones · Tendido eléctrico", result.rubro());
        assertEquals("Nivel 2", result.nivel());
        assertEquals("m", result.unidad());
    }

    @Test
    void rejectsParentWithoutSubrubro() {
        RubroObra parent = new RubroObra();
        parent.setId(32L);
        parent.setProyecto(proyecto);
        parent.setRubro("Instalaciones");
        when(rubros.findById(32L)).thenReturn(Optional.of(parent));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, request(32L)));
        verify(restricciones, never()).save(any());
    }

    @Test
    void rejectsSubrubroFromAnotherProject() {
        Proyecto otherProject = new Proyecto();
        otherProject.setId(20L);
        RubroObra foreignActivity = new RubroObra();
        foreignActivity.setId(33L);
        foreignActivity.setProyecto(otherProject);
        foreignActivity.setSubRubro("Tendido eléctrico");
        when(rubros.findById(33L)).thenReturn(Optional.of(foreignActivity));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, request(33L)));
        verify(restricciones, never()).save(any());
    }

    private RestriccionCreateDto request(Long rubroId) {
        return new RestriccionCreateDto("Esperar inspección", rubroId, null, null, null, null, null,
            null, null, null, null, null, null, null);
    }
}