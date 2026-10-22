package com.vortise.gestion.infrastructure.config;

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

/** Seeds the initial plan catalog and migrates only recognized legacy prices. */
@Component
@Order(40)
public class PlanSuscripcionSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(PlanSuscripcionSeeder.class);

    private final PlanSuscripcionRepository planes;
    private final Map<String, String> preciosConfigurados;
    private final SeedRunPolicy seedRunPolicy;

    @Autowired
    public PlanSuscripcionSeeder(PlanSuscripcionRepository planes,
            @Value("${app.stripe.price-basic:}") String priceBasic,
            @Value("${app.stripe.price-pro:}") String pricePro, SeedRunPolicy seedRunPolicy) {
        this.planes = planes;
        this.preciosConfigurados = Map.of("BASIC", priceBasic == null ? "" : priceBasic.trim(), "PRO", pricePro == null ? "" : pricePro.trim());
        this.seedRunPolicy = seedRunPolicy;
    }

    public PlanSuscripcionSeeder(PlanSuscripcionRepository planes, String priceBasic, String pricePro) {
        this(planes, priceBasic, pricePro, () -> true);
    }

    @Override
    public void run(String... args) {
        boolean crearPlanesAusentes = seedRunPolicy.shouldRunSeeders();
        sincronizar(new PlanSuscripcion("BASIC", "Basic", 900, "usd", 7, 1, List.of(
            "Gestión de empresas y obras",
            "Cronograma y Gantt",
            "Restricciones y tareas",
            "Registro de horas",
            "Dashboard de obra",
            "7 días de prueba gratis")), 100, crearPlanesAusentes);
        sincronizar(new PlanSuscripcion("PRO", "Pro", 1900, "usd", 0, 2, List.of(
            "Todo lo incluido en Basic",
            "Reportes y rendimientos",
            "Importación desde Excel",
            "Gastos fijos y cashflow",
            "Soporte prioritario")), 200, crearPlanesAusentes);
    }

    private void sincronizar(PlanSuscripcion oferta, long precioLegacy, boolean crearSiFalta) {
        PlanSuscripcion plan = planes.findByCodigo(oferta.getCodigo()).orElse(null);
        if (plan == null) {
            if (!crearSiFalta) return;
            plan = oferta;
            plan.setStripePriceId(precioConfigurado(plan.getCodigo()));
            planes.save(plan);
        } else {
            boolean precioLegacyReconocido = plan.getPrecioMensualCentavos() == precioLegacy;
            boolean precioOficial = plan.getPrecioMensualCentavos() == oferta.getPrecioMensualCentavos()
                && oferta.getMoneda().equalsIgnoreCase(plan.getMoneda());
            if (precioLegacyReconocido || precioOficial) {
                String priceId = precioConfigurado(plan.getCodigo());
                boolean changed = false;
                if (precioLegacyReconocido) {
                    plan.setPrecioMensualCentavos(oferta.getPrecioMensualCentavos());
                    plan.setMoneda(oferta.getMoneda());
                    changed = true;
                }
                if (!java.util.Objects.equals(plan.getStripePriceId(), priceId)) {
                    plan.setStripePriceId(priceId);
                    changed = true;
                }
                if (changed) planes.save(plan);
            }
        }

        if (plan.getStripePriceId() == null || plan.getStripePriceId().isBlank()) {
            log.warn("Plan {} has no Stripe Price ID; configure {} before enabling checkout",
                plan.getCodigo(), variablePrecio(plan.getCodigo()));
        }
    }

    private String precioConfigurado(String codigo) {
        String configurado = preciosConfigurados.getOrDefault(codigo, "");
        return configurado.isEmpty() ? null : configurado;
    }

    private String variablePrecio(String codigo) {
        return "STRIPE_PRICE_" + codigo;
    }
}
