package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.service.ProyectoConsultaService;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.service.CreateProyectoUseCase;
import com.vortise.gestion.application.service.DeleteProyectoUseCase;
import com.vortise.gestion.application.service.UpdateProyectoUseCase;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProyectoDemoResetAvailabilityTest {
    @Test
    void disabledResetDoesNotInvokeDemoSeeder() {
        DataSeeder seeder = org.mockito.Mockito.mock(DataSeeder.class);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false, false);
        ProyectoController controller = new ProyectoController(
            org.mockito.Mockito.mock(CreateProyectoUseCase.class),
            org.mockito.Mockito.mock(UpdateProyectoUseCase.class),
            org.mockito.Mockito.mock(DeleteProyectoUseCase.class), seeder, policy, org.mockito.Mockito.mock(ProyectoConsultaService.class));

        assertThrows(FeatureUnavailableException.class, controller::resetDemo);
        verify(seeder, never()).resetDemoData();
    }
}
