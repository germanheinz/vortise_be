package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.controller.RegistroHorasController;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.RegistroHorasCreateDto;
import com.vortise.gestion.application.dto.RegistroHorasDto;
import com.vortise.gestion.application.dto.RegistroHorasUpdateDto;
import com.vortise.gestion.application.service.RegistroHorasService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RegistroHorasControllerTest {
    @Test
    void mutationsDelegateToServiceAndReturnExistingStatuses() {
        var servicio = mock(RegistroHorasService.class);
        var controller = new RegistroHorasController(servicio);
        var inicio = OffsetDateTime.parse("2026-11-26T08:00:00Z");
        var alta = new RegistroHorasCreateDto(null, null, null, null, null, inicio, inicio.plusHours(1),
                null, null, null, null, null, null, null, null);
        var edicion = new RegistroHorasUpdateDto(null, null, null, null, null, inicio, inicio.plusHours(2),
                null, null, null, null, null, null, null, null);
        var registro = new RegistroHorasDto(9L, 7L, "Obra", null, null, null, null, null,
                inicio, inicio.plusHours(1), 1.0, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        when(servicio.create(7L, alta)).thenReturn(registro);
        when(servicio.update(9L, edicion)).thenReturn(registro);

        var creado = controller.create(7L, alta);
        var editado = controller.update(9L, edicion);
        var eliminado = controller.delete(9L);

        assertEquals(HttpStatus.CREATED, creado.getStatusCode());
        assertSame(registro, creado.getBody());
        assertEquals(HttpStatus.OK, editado.getStatusCode());
        assertSame(registro, editado.getBody());
        assertEquals(HttpStatus.NO_CONTENT, eliminado.getStatusCode());
        assertNull(eliminado.getBody());
        verify(servicio).delete(9L);
    }
}
