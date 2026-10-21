package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanSuscripcionSeederTest {
    private final PlanSuscripcionRepository planes = mock(PlanSuscripcionRepository.class);
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final List<PlanSuscripcion> guardados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(planes.findByCodigo(any())).thenAnswer(inv -> guardados.stream().filter(p -> p.getCodigo().equals(inv.getArgument(0))).findFirst());
        when(planes.save(any(PlanSuscripcion.class))).thenAnswer(inv -> {
            PlanSuscripcion p = inv.getArgument(0);
            if (!guardados.contains(p)) guardados.add(p);
            return p;
        });
        when(planes.findByActivoTrueOrderByNivelAsc()).thenAnswer(inv -> List.copyOf(guardados));
    }

    @Test
    void createsBasicWithTrialAndProWithoutOnceEvenOnRepeatedStartup() {
        PlanSuscripcionSeeder seeder = new PlanSuscripcionSeeder(planes, stripe, "", "");
        seeder.run();
        seeder.run();
        assertEquals(List.of("BASIC", "PRO"), guardados.stream().map(PlanSuscripcion::getCodigo).toList());
        assertEquals(7, guardados.get(0).getDiasPrueba());
        assertEquals(0, guardados.get(1).getDiasPrueba());
        assertEquals(100, guardados.get(0).getPrecioMensualCentavos());
        assertEquals(200, guardados.get(1).getPrecioMensualCentavos());
    }

    @Test
    void withoutStripeConfigNoPriceIsLinked() {
        new PlanSuscripcionSeeder(planes, stripe, "", "").run();
        verify(stripe, never()).asegurarPrecioMensual(anyString(), anyString(), anyLong(), anyString());
        guardados.forEach(p -> assertEquals(null, p.getStripePriceId()));
    }

    @Test
    void configuredPriceWinsAndMissingOneIsCreatedByLookupKey() {
        when(stripe.isConfigured()).thenReturn(true);
        when(stripe.asegurarPrecioMensual("gestion_pro_mensual", "Gestión de Obras Pro", 200, "usd")).thenReturn("price_created");
        new PlanSuscripcionSeeder(planes, stripe, "price_env_basic", "").run();
        assertEquals("price_env_basic", guardados.get(0).getStripePriceId());
        assertEquals("price_created", guardados.get(1).getStripePriceId());
        verify(stripe, never()).asegurarPrecioMensual(org.mockito.ArgumentMatchers.eq("gestion_basic_mensual"), anyString(), anyLong(), anyString());
    }

    @Test
    void stripeFailureDoesNotBreakStartup() {
        when(stripe.isConfigured()).thenReturn(true);
        when(stripe.asegurarPrecioMensual(anyString(), anyString(), anyLong(), anyString())).thenThrow(new RuntimeException("down"));
        new PlanSuscripcionSeeder(planes, stripe, "", "").run();
        guardados.forEach(p -> assertEquals(null, p.getStripePriceId()));
    }

    @Test
    void existingEditedPlanIsNotOverwritten() {
        PlanSuscripcion editado = new PlanSuscripcion("BASIC", "Basic editado", 999, "usd", 14, 1, List.of("Custom"));
        guardados.add(editado);
        new PlanSuscripcionSeeder(planes, stripe, "", "").run();
        assertEquals(Optional.of(editado), planes.findByCodigo("BASIC"));
        assertEquals(999, editado.getPrecioMensualCentavos());
        assertEquals(14, editado.getDiasPrueba());
    }
}
