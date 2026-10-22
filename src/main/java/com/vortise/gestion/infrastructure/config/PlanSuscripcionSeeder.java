package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Creates the plan catalog once and links Stripe prices; never overwrites edited plans. */
@Component
@Order(40)
public class PlanSuscripcionSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(PlanSuscripcionSeeder.class);

    private final PlanSuscripcionRepository planes;
    private final StripeGateway stripe;
    private final Map<String, String> preciosConfigurados;
    private final SeedRunPolicy seedRunPolicy;

    @Autowired
    public PlanSuscripcionSeeder(PlanSuscripcionRepository planes, StripeGateway stripe,
            @Value("${app.stripe.price-basic:}") String priceBasic,
            @Value("${app.stripe.price-pro:}") String pricePro, SeedRunPolicy seedRunPolicy) {
        this.planes = planes;
        this.stripe = stripe;
        this.preciosConfigurados = Map.of("BASIC", priceBasic == null ? "" : priceBasic.trim(), "PRO", pricePro == null ? "" : pricePro.trim());
        this.seedRunPolicy = seedRunPolicy;
    }

    public PlanSuscripcionSeeder(PlanSuscripcionRepository planes, StripeGateway stripe, String priceBasic, String pricePro) {
        this(planes, stripe, priceBasic, pricePro, () -> true);
    }

    @Override
    public void run(String... args) {
        if (!seedRunPolicy.shouldRunSeeders()) return;
        asegurar(new PlanSuscripcion("BASIC", "Basic", 100, "usd", 7, 1, List.of(
            "Gestión de empresas y obras",
            "Cronograma y Gantt",
            "Restricciones y tareas",
            "Registro de horas",
            "Dashboard de obra",
            "7 días de prueba gratis")));
        asegurar(new PlanSuscripcion("PRO", "Pro", 200, "usd", 0, 2, List.of(
            "Todo lo incluido en Basic",
            "Reportes y rendimientos",
            "Importación desde Excel",
            "Gastos fijos y cashflow",
            "Soporte prioritario")));
        if (stripe.isConfigured()) planes.findByActivoTrueOrderByNivelAsc().forEach(this::vincularPrecio);
    }

    private void asegurar(PlanSuscripcion plan) {
        if (planes.findByCodigo(plan.getCodigo()).isEmpty()) planes.save(plan);
    }

    private void vincularPrecio(PlanSuscripcion plan) {
        String configurado = preciosConfigurados.getOrDefault(plan.getCodigo(), "");
        try {
            String priceId = !configurado.isEmpty() ? configurado
                : plan.getStripePriceId() != null ? plan.getStripePriceId()
                : stripe.asegurarPrecioMensual("gestion_" + plan.getCodigo().toLowerCase() + "_mensual",
                    "Gestión de Obras " + plan.getNombre(), plan.getPrecioMensualCentavos(), plan.getMoneda());
            if (!priceId.equals(plan.getStripePriceId())) {
                plan.setStripePriceId(priceId);
                planes.save(plan);
            }
        } catch (RuntimeException ex) {
            log.warn("Could not link a Stripe price to plan {}; it stays unavailable until the next startup", plan.getCodigo());
        }
    }
}
