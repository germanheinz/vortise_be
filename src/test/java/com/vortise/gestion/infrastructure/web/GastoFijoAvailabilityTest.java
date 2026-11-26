package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.controller.GastoFijoController;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.service.GastoFijoService;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GastoFijoAvailabilityTest {
    @Test
    void disabledFeatureRejectsReadBeforeAccessingGlobalRepository() {
        var servicio = org.mockito.Mockito.mock(GastoFijoService.class);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false);
        GastoFijoController controller = new GastoFijoController(servicio, policy);

        assertThrows(FeatureUnavailableException.class, controller::getAll);
        verify(servicio, never()).listar();
    }

    @Test
    void disabledFeatureRejectsAllMutationsBeforeUseCases() {
        var servicio = org.mockito.Mockito.mock(GastoFijoService.class);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false);
        GastoFijoController controller = new GastoFijoController(servicio, policy);

        assertThrows(FeatureUnavailableException.class, () -> controller.create(null));
        assertThrows(FeatureUnavailableException.class, () -> controller.update(1L, null));
        assertThrows(FeatureUnavailableException.class, () -> controller.delete(1L));
        verify(servicio, never()).guardar(org.mockito.ArgumentMatchers.any());
        verify(servicio, never()).eliminar(org.mockito.ArgumentMatchers.anyLong());
    }
}
