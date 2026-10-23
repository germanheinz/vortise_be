package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.usecase.DeleteGastoFijoUseCase;
import com.vortise.gestion.application.usecase.GetGastosFijosUseCase;
import com.vortise.gestion.application.usecase.SaveGastoFijoUseCase;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GastoFijoAvailabilityTest {
    @Test
    void disabledFeatureRejectsReadBeforeAccessingGlobalRepository() {
        GetGastosFijosUseCase get = org.mockito.Mockito.mock(GetGastosFijosUseCase.class);
        SaveGastoFijoUseCase save = org.mockito.Mockito.mock(SaveGastoFijoUseCase.class);
        DeleteGastoFijoUseCase delete = org.mockito.Mockito.mock(DeleteGastoFijoUseCase.class);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false, false);
        GastoFijoController controller = new GastoFijoController(get, save, delete, policy);

        assertThrows(FeatureUnavailableException.class, controller::getAll);
        verify(get, never()).execute();
    }

    @Test
    void disabledFeatureRejectsAllMutationsBeforeUseCases() {
        GetGastosFijosUseCase get = org.mockito.Mockito.mock(GetGastosFijosUseCase.class);
        SaveGastoFijoUseCase save = org.mockito.Mockito.mock(SaveGastoFijoUseCase.class);
        DeleteGastoFijoUseCase delete = org.mockito.Mockito.mock(DeleteGastoFijoUseCase.class);
        ProductionFeaturePolicy policy = new ProductionFeaturePolicy(new MockEnvironment(), false, false, false, false);
        GastoFijoController controller = new GastoFijoController(get, save, delete, policy);

        assertThrows(FeatureUnavailableException.class, () -> controller.create(null));
        assertThrows(FeatureUnavailableException.class, () -> controller.update(1L, null));
        assertThrows(FeatureUnavailableException.class, () -> controller.delete(1L));
        verify(save, never()).execute(org.mockito.ArgumentMatchers.any());
        verify(delete, never()).execute(org.mockito.ArgumentMatchers.anyLong());
    }
}
