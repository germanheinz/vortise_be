package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.application.usecase.StripeWebhookUseCase;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import com.vortise.gestion.infrastructure.config.*;
import jakarta.persistence.EntityManager;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnabledIfSystemProperty(named = "payments.pg.enabled", matches = "true")
class PaymentsPostgresTest {
    @MockBean DataSeeder dataSeeder;
    @MockBean UsuarioDemoSeeder usuarioDemoSeeder;
    @MockBean PlanSuscripcionSeeder planSeeder;
    @MockBean SuperAdminSeeder superSeeder;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired PagoSuscripcionRepository pagos;
    @Autowired SuscripcionRepository suscripciones;
    @Autowired UsuarioEmpresaRepository usuarios;
    @MockBean StripeGateway stripe;
    @Autowired StripeWebhookUseCase webhook;

    @DynamicPropertySource
    static void isolated(DynamicPropertyRegistry r) throws Exception {
        // Only the explicitly approved disposable local container, no inherited datasource.
        var lines = Files.readAllLines(Path.of(".env.payments-local-06"));
        String password = lines.stream().filter(s -> s.startsWith("POSTGRES_PASSWORD=")).findFirst().orElseThrow().substring(18);
        byte[] key = new byte[64]; new SecureRandom().nextBytes(key);
        r.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:15433/gestion_payments_test");
        r.add("spring.datasource.username", () -> "gestion_payments_test");
        r.add("spring.datasource.password", () -> password);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> true);
        r.add("spring.sql.init.mode", () -> "never");
        r.add("spring.h2.console.enabled", () -> false);
        r.add("app.security.jwt-secret", () -> Base64.getEncoder().encodeToString(key));
        for (String p : java.util.List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) r.add("app.stripe." + p, () -> "");
    }

    @Test
    void delayedFailedSnapshotCannotOverwriteACommittedPaidInvoice() throws Exception {
        var tx = new TransactionTemplate(tm);
        String suffix = java.util.UUID.randomUUID().toString();
        String customer = "cus_concurrent_" + suffix;
        String invoiceId = "in_concurrent_" + suffix;
        Long[] ids = tx.execute(status -> {
            var company = new Empresa("Concurrent payments test", null); em.persist(company);
            var user = new UsuarioEmpresa(company, suffix + "@example.invalid", "Concurrent", "not-a-login-password", RolEmpresa.ADMINISTRADOR); em.persist(user);
            var subscription = Suscripcion.paraUsuario(user, customer); em.persist(subscription); em.flush();
            return new Long[]{company.getId(), user.getId(), subscription.getId()};
        });
        var oldRead = new java.util.concurrent.CountDownLatch(1);
        var releaseOld = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        Instant date = Instant.parse("2026-10-27T00:00:00Z");
        var failed = new StripeGateway.FacturaStripe(invoiceId, null, customer, 5000, "usd", "FALLIDO", date,
            1L, date.plusSeconds(86400), "open", null, date, date.plusSeconds(2592000));
        var paid = new StripeGateway.FacturaStripe(invoiceId, null, customer, 5000, "usd", "PAGADO", date.plusSeconds(3600),
            2L, null, "paid", date.plusSeconds(3600), date, date.plusSeconds(2592000));
        var current = new java.util.concurrent.atomic.AtomicReference<>(failed);
        when(stripe.isConfigured()).thenReturn(true);
        when(stripe.obtenerFactura(invoiceId)).thenAnswer(i -> current.get());
        when(stripe.parsearWebhook("delayed", "signature")).thenAnswer(i -> {
            var snapshot = failed;
            oldRead.countDown();
            if (!releaseOld.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timed out");
            return new StripeGateway.EventoStripe("evt_old_" + suffix, "invoice.payment_failed", null, snapshot);
        });
        when(stripe.parsearWebhook("committed", "signature")).thenReturn(
            new StripeGateway.EventoStripe("evt_paid_" + suffix, "invoice.paid", null, paid));
        try {
            var delayed = executor.submit(() -> webhook.procesar("delayed", "signature"));
            assertTrue(oldRead.await(20, java.util.concurrent.TimeUnit.SECONDS));
            current.set(paid);
            webhook.procesar("committed", "signature"); // Transaction B commits before A obtains its customer lock.
            releaseOld.countDown();
            delayed.get(20, java.util.concurrent.TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                var result = pagos.findByStripeInvoiceId(invoiceId).orElseThrow();
                assertEquals("PAGADO", result.getEstado());
                assertEquals(2L, result.getIntentos());
                assertNull(result.getProximoIntento());
                assertEquals(date.plusSeconds(3600), result.getFechaPago());
                assertEquals(1L, em.createQuery("select count(p) from PagoSuscripcion p where p.stripeInvoiceId = :id", Long.class)
                    .setParameter("id", invoiceId).getSingleResult());
            });
        } finally {
            releaseOld.countDown(); executor.shutdown();
            assertTrue(executor.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS));
            // Delete only rows this test created, exclusively in the approved disposable PG database.
            tx.executeWithoutResult(status -> {
                em.createQuery("delete from StripeEventoProcesado e where e.eventId in :ids")
                    .setParameter("ids", java.util.List.of("evt_old_" + suffix, "evt_paid_" + suffix)).executeUpdate();
                em.createQuery("delete from PagoSuscripcion p where p.stripeInvoiceId = :id").setParameter("id", invoiceId).executeUpdate();
                em.createQuery("delete from Suscripcion s where s.id = :id").setParameter("id", ids[2]).executeUpdate();
                em.createQuery("delete from UsuarioEmpresa u where u.id = :id").setParameter("id", ids[1]).executeUpdate();
                em.createQuery("delete from Empresa e where e.id = :id").setParameter("id", ids[0]).executeUpdate();
            });
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void renewalFailureRecoveryCancellationAndReplayPreserveInvoiceIdentity() {
        String suffix = java.util.UUID.randomUUID().toString();
        String customer = "cus_cycle_" + suffix;
        String remoteSubscription = "sub_cycle_" + suffix;
        var company = new Empresa("Cycle rollback test", null); em.persist(company);
        var user = new UsuarioEmpresa(company, suffix + "@example.invalid", "Cycle", "not-a-login-password", RolEmpresa.ADMINISTRADOR); em.persist(user);
        var local = Suscripcion.paraUsuario(user, customer); em.persist(local); em.flush();
        when(stripe.isConfigured()).thenReturn(true);
        Instant start = Instant.parse("2026-10-27T00:00:00Z");
        Instant end = start.plusSeconds(30L * 86400);
        var first = new StripeGateway.FacturaStripe("in_first_" + suffix, remoteSubscription, customer, 5000, "usd", "PAGADO", start,
            1L, null, "paid", start, start, end);
        when(stripe.obtenerSuscripcion(remoteSubscription)).thenReturn(new StripeGateway.SuscripcionStripe(remoteSubscription, customer, "active", null, null, end, false, start));
        when(stripe.parsearWebhook("first", "signature")).thenReturn(new StripeGateway.EventoStripe("evt_first_" + suffix, "invoice.paid", remoteSubscription, first));
        when(stripe.obtenerFactura(first.id())).thenReturn(first);
        webhook.procesar("first", "signature");

        String renewalId = "in_renew_" + suffix;
        var failed = new StripeGateway.FacturaStripe(renewalId, remoteSubscription, customer, 5000, "usd", "FALLIDO", end,
            1L, end.plusSeconds(86400), "open", null, end, end.plusSeconds(30L * 86400));
        when(stripe.obtenerSuscripcion(remoteSubscription)).thenReturn(new StripeGateway.SuscripcionStripe(remoteSubscription, customer, "past_due", null, null, end.plusSeconds(30L * 86400), false, start));
        when(stripe.parsearWebhook("failure", "signature")).thenReturn(new StripeGateway.EventoStripe("evt_failure_" + suffix, "invoice.payment_failed", remoteSubscription, failed));
        when(stripe.obtenerFactura(renewalId)).thenReturn(failed);
        webhook.procesar("failure", "signature");
        var payment = pagos.findByStripeInvoiceId(renewalId).orElseThrow();
        Long paymentId = payment.getId();
        assertEquals("past_due", local.getEstado());
        assertEquals("FALLIDO", payment.getEstado());
        assertEquals(1L, payment.getIntentos());
        assertNotNull(payment.getProximoIntento());

        var recovered = new StripeGateway.FacturaStripe(renewalId, remoteSubscription, customer, 5000, "usd", "PAGADO", end.plusSeconds(3600),
            2L, null, "paid", end.plusSeconds(3600), end, end.plusSeconds(30L * 86400));
        when(stripe.obtenerSuscripcion(remoteSubscription)).thenReturn(new StripeGateway.SuscripcionStripe(remoteSubscription, customer, "active", null, null, end.plusSeconds(30L * 86400), false, start));
        var recoveryEvent = new StripeGateway.EventoStripe("evt_recover_" + suffix, "invoice.paid", remoteSubscription, recovered);
        when(stripe.parsearWebhook("recovery", "signature")).thenReturn(recoveryEvent);
        when(stripe.obtenerFactura(renewalId)).thenReturn(recovered);
        webhook.procesar("recovery", "signature");
        webhook.procesar("recovery", "signature");
        assertEquals(paymentId, pagos.findByStripeInvoiceId(renewalId).orElseThrow().getId());
        assertEquals("PAGADO", payment.getEstado());
        assertEquals(2L, payment.getIntentos());
        assertNull(payment.getProximoIntento());
        assertEquals("active", local.getEstado());
        assertEquals(2L, em.createQuery("select count(p) from PagoSuscripcion p where p.suscripcion.id = :id", Long.class)
            .setParameter("id", local.getId()).getSingleResult());

        when(stripe.obtenerSuscripcion(remoteSubscription)).thenReturn(new StripeGateway.SuscripcionStripe(remoteSubscription, customer, "active", null, null, end.plusSeconds(30L * 86400), true, start));
        when(stripe.parsearWebhook("cancel", "signature")).thenReturn(new StripeGateway.EventoStripe("evt_cancel_" + suffix, "customer.subscription.updated", remoteSubscription, null));
        webhook.procesar("cancel", "signature");
        assertTrue(local.isCancelaAlFinal());
        when(stripe.obtenerSuscripcion(remoteSubscription)).thenReturn(new StripeGateway.SuscripcionStripe(remoteSubscription, customer, "canceled", null, null, end.plusSeconds(30L * 86400), false, start));
        when(stripe.parsearWebhook("canceled", "signature")).thenReturn(new StripeGateway.EventoStripe("evt_canceled_" + suffix, "customer.subscription.deleted", remoteSubscription, null));
        webhook.procesar("canceled", "signature");
        assertEquals("canceled", local.getEstado());
        assertEquals(2L, em.createQuery("select count(p) from PagoSuscripcion p where p.suscripcion.id = :id", Long.class)
            .setParameter("id", local.getId()).getSingleResult());
        // This scenario rolls back instead of polluting the user's local panel.
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void postgresSupportsOptionalFiltersAndPersistsRefundsWithoutH2() {
        var tx = new TransactionTemplate(tm);
        Long id = tx.execute(status -> {
            var company = new Empresa("Payments synthetic " + java.util.UUID.randomUUID(), null); em.persist(company);
            var user = new UsuarioEmpresa(company, java.util.UUID.randomUUID() + "@example.invalid", "Payments test", "not-a-login-password", RolEmpresa.ADMINISTRADOR); em.persist(user);
            var subscription = Suscripcion.paraUsuario(user, "cus_test_" + java.util.UUID.randomUUID());
            subscription.actualizarDesdeStripe("sub_test_" + java.util.UUID.randomUUID(), null, "active", null, Instant.parse("2026-12-01T00:00:00Z"), false);
            em.persist(subscription);
            var payment = new PagoSuscripcion(subscription, "in_test_" + java.util.UUID.randomUUID());
            payment.registrar(9900, "usd", "PAGADO", Instant.parse("2026-10-27T00:00:00Z"));
            payment.registrarDevolucion("re_test_" + java.util.UUID.randomUUID(), 3000, "succeeded", Instant.now());
            em.persist(payment); em.flush(); return payment.getId();
        });
        tx.executeWithoutResult(status -> {
            em.clear();
            var payment = em.find(PagoSuscripcion.class, id);
            assertEquals(3000, payment.getMontoDevueltoCentavos());
            assertEquals(6900, payment.getMontoNetoCentavos());
            for (String state : new String[]{null, "PaGaDo"}) {
                for (Instant start : new Instant[]{null, Instant.parse("2026-10-01T00:00:00Z")}) {
                    for (Instant end : new Instant[]{null, Instant.parse("2026-11-01T00:00:00Z")}) {
                        assertTrue(pagos.buscarSuperAdmin(null, state, start, end, PageRequest.of(0, 100))
                            .stream().anyMatch(p -> p.getId().equals(id)));
                    }
                }
            }
            assertFalse(suscripciones.buscarSuperAdmin(null, "AcTiVe", null, PageRequest.of(0, 100)).isEmpty());
            assertFalse(suscripciones.buscarSuperAdmin(null, null, null, PageRequest.of(0, 100)).isEmpty());
            assertFalse(usuarios.buscarSuperAdmin(null, null, null, "", PageRequest.of(0, 100)).isEmpty());
            assertTrue(pagos.resumirRecientesSuperAdmin(null, Instant.parse("2026-10-01T00:00:00Z"))
                .stream().anyMatch(row -> ((Number) row[4]).longValue() >= 3000));
        });
    }
}
