package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanSuscripcionSeederTest {
    private final PlanSuscripcionRepository planes = mock(PlanSuscripcionRepository.class);
    private final List<PlanSuscripcion> guardados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(planes.findByCodigo(any())).thenAnswer(inv -> guardados.stream().filter(p -> p.getCodigo().equals(inv.getArgument(0))).findFirst());
        when(planes.save(any(PlanSuscripcion.class))).thenAnswer(inv -> {
            PlanSuscripcion p = inv.getArgument(0);
            if (!guardados.contains(p)) guardados.add(p);
            return p;
        });
    }

    @Test
    void createsPlansAtOfficialPricesWithCurrentTrialsAndBenefits() {
        PlanSuscripcionSeeder seeder = new PlanSuscripcionSeeder(planes, "", "");
        seeder.run();
        seeder.run();
        assertEquals(List.of("BASIC", "PRO"), guardados.stream().map(plan -> plan.getCodigo()).toList());
        assertEquals(7, guardados.get(0).getDiasPrueba());
        assertEquals(0, guardados.get(1).getDiasPrueba());
        assertEquals(5000, guardados.get(0).getPrecioMensualCentavos());
        assertEquals(9900, guardados.get(1).getPrecioMensualCentavos());
        assertEquals("usd", guardados.get(0).getMoneda());
        assertEquals("usd", guardados.get(1).getMoneda());
        assertEquals("Gestión de empresas y obras", guardados.get(0).getBeneficios().get(0));
        assertEquals("Soporte prioritario", guardados.get(1).getBeneficios().get(4));
    }

    @Test
    void configuredEnvironmentPriceIdsAreUsedForNewPlans() {
        new PlanSuscripcionSeeder(planes, "price_basic_new", "price_pro_new").run();
        assertEquals("price_basic_new", guardados.get(0).getStripePriceId());
        assertEquals("price_pro_new", guardados.get(1).getStripePriceId());
    }

    @Test
    void configuredPriceIdsAreAppliedAfterInitialCatalogSeed() {
        new PlanSuscripcionSeeder(planes, "", "").run();
        assertEquals(null, guardados.get(0).getStripePriceId());
        assertEquals(null, guardados.get(1).getStripePriceId());

        new PlanSuscripcionSeeder(planes, "price_basic_new", "price_pro_new", () -> false).run();

        assertEquals("price_basic_new", guardados.get(0).getStripePriceId());
        assertEquals("price_pro_new", guardados.get(1).getStripePriceId());
    }

    @Test
    void migratesOnlyExactLegacyPricesAndClearsUnconfiguredLegacyPriceIds() {
        PlanSuscripcion basic = new PlanSuscripcion("BASIC", "Basic manual", 100, "usd", 7, 1, List.of("Custom"));
        basic.setStripePriceId("price_basic_old");
        PlanSuscripcion pro = new PlanSuscripcion("PRO", "Pro manual", 200, "usd", 0, 2, List.of("Custom"));
        pro.setStripePriceId("price_pro_old");
        guardados.addAll(List.of(basic, pro));

        new PlanSuscripcionSeeder(planes, "price_basic_new", "").run();

        assertEquals(5000, basic.getPrecioMensualCentavos());
        assertEquals("price_basic_new", basic.getStripePriceId());
        assertEquals("Basic manual", basic.getNombre());
        assertEquals(List.of("Custom"), basic.getBeneficios());
        assertEquals(9900, pro.getPrecioMensualCentavos());
        assertEquals(null, pro.getStripePriceId());
    }

    @Test
    void upgradesPreviousOfficialUsdPricesWithoutChangingTrialOrBenefits() {
        PlanSuscripcion basic = new PlanSuscripcion("BASIC", "Basic", 900, "usd", 7, 1, List.of("Custom"));
        PlanSuscripcion pro = new PlanSuscripcion("PRO", "Pro", 1900, "usd", 0, 2, List.of("Custom"));
        basic.setStripePriceId("price_old");
        guardados.addAll(List.of(basic, pro));
        new PlanSuscripcionSeeder(planes, "", "", () -> false).run();
        assertEquals(5000, basic.getPrecioMensualCentavos());
        assertEquals(9900, pro.getPrecioMensualCentavos());
        assertEquals(null, basic.getStripePriceId());
        assertEquals(7, basic.getDiasPrueba());
        assertEquals(List.of("Custom"), basic.getBeneficios());
    }

    @Test
    void preservesManuallyPricedPlansAndTheirPriceIdsEvenWhenEnvironmentIdsExist() {
        PlanSuscripcion editado = new PlanSuscripcion("BASIC", "Basic editado", 999, "usd", 14, 1, List.of("Custom"));
        editado.setStripePriceId("price_manual");
        guardados.add(editado);

        new PlanSuscripcionSeeder(planes, "price_basic_new", "").run();

        assertEquals(999, editado.getPrecioMensualCentavos());
        assertEquals(14, editado.getDiasPrueba());
        assertEquals("price_manual", editado.getStripePriceId());
        assertEquals(List.of("Custom"), editado.getBeneficios());
    }

    @Test
    void populatedDatabaseStillMigratesExistingLegacyPlanWithoutCreatingMissingPlans() {
        SeedRunPolicy skipCreation = () -> false;
        PlanSuscripcion basic = new PlanSuscripcion("BASIC", "Basic", 100, "usd", 7, 1, List.of("Custom"));
        guardados.add(basic);

        new PlanSuscripcionSeeder(planes, "", "", skipCreation).run();

        assertEquals(5000, basic.getPrecioMensualCentavos());
        assertEquals(List.of("BASIC"), guardados.stream().map(plan -> plan.getCodigo()).toList());
        verify(planes, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.argThat(plan -> plan.getCodigo().equals("PRO")));
    }

    @Test
    void plansWithoutConfiguredPricesKeepNullStripePriceId() {
        new PlanSuscripcionSeeder(planes, "", "").run();
        guardados.forEach(p -> assertEquals(null, p.getStripePriceId()));
    }
}
