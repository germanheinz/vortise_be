package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.CheckoutRequestDto;
import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.application.port.StripeGateway.CheckoutStripe;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.TitularSuscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class SuscripcionUseCaseTest {
    private final PlanSuscripcionRepository planes = mock(PlanSuscripcionRepository.class);
    private final SuscripcionRepository suscripciones = mock(SuscripcionRepository.class);
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final PagoSuscripcionRepository pagos = mock(PagoSuscripcionRepository.class);
    private final CurrentTenantService tenant = mock(CurrentTenantService.class);
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final SuscripcionConsultaService consulta = new SuscripcionConsultaService(suscripciones, pagos);
    private final SuscripcionUseCase useCase = new SuscripcionUseCase(planes, suscripciones, usuarios, consulta, tenant, stripe, "https://app.example/");

    private final Empresa empresa = withId(new Empresa("Empresa", null), 10L);
    private final UsuarioEmpresa usuario = withId(new UsuarioEmpresa(empresa, "u@example.invalid", "Usuario", "hash", RolEmpresa.JEFE_DE_OBRA), 7L);
    private final PlanSuscripcion basic = plan("BASIC", 7, 1, "price_basic");
    private final PlanSuscripcion pro = plan("PRO", 0, 2, "price_pro");

    @BeforeEach
    void setUp() {
        when(stripe.isConfigured()).thenReturn(true);
        when(tenant.usuarioId()).thenReturn(7L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(planes.findByCodigo("BASIC")).thenReturn(Optional.of(basic));
        when(planes.findByCodigo("PRO")).thenReturn(Optional.of(pro));
        when(suscripciones.findByUsuarioId(any())).thenReturn(Optional.empty());
        when(suscripciones.findByEmpresaId(any())).thenReturn(Optional.empty());
        when(suscripciones.save(any(Suscripcion.class))).thenAnswer(inv -> withId(inv.getArgument(0), 99L));
        when(stripe.crearCliente(anyString(), anyString(), any())).thenReturn("cus_1");
        when(stripe.crearCheckout(any())).thenReturn("https://checkout.stripe.test/s");
    }

    @Test
    void basicCheckoutForNewUserCreatesCustomerAndAppliesTrial() {
        assertEquals("https://checkout.stripe.test/s", useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.USUARIO)).url());
        CheckoutStripe sent = captureCheckout();
        assertEquals("cus_1", sent.customerId());
        assertEquals("price_basic", sent.priceId());
        assertEquals(7, sent.diasPrueba());
        assertEquals("https://app.example/planes?estado=exito", sent.successUrl());
        assertEquals("https://app.example/planes?estado=cancelado", sent.cancelUrl());
    }

    @Test
    void proCheckoutHasNoTrial() {
        useCase.checkout(new CheckoutRequestDto("PRO", TitularSuscripcion.USUARIO));
        assertNull(captureCheckout().diasPrueba());
    }

    @Test
    void trialIsNotGrantedTwice() {
        Suscripcion previa = Suscripcion.paraUsuario(usuario, "cus_old");
        previa.actualizarDesdeStripe("sub_old", basic, "canceled", Instant.now().minus(30, ChronoUnit.DAYS), null, false);
        when(suscripciones.findByUsuarioId(7L)).thenReturn(Optional.of(previa));
        useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.USUARIO));
        CheckoutStripe sent = captureCheckout();
        assertNull(sent.diasPrueba());
        assertEquals("cus_old", sent.customerId());
        verify(stripe, never()).crearCliente(anyString(), anyString(), any());
    }

    @Test
    void activeSubscriptionCannotCheckoutAgain() {
        Suscripcion activa = Suscripcion.paraUsuario(usuario, "cus_1");
        activa.actualizarDesdeStripe("sub_1", basic, "active", null, Instant.now().plus(10, ChronoUnit.DAYS), false);
        when(suscripciones.findByUsuarioId(7L)).thenReturn(Optional.of(activa));
        assertThrows(IllegalStateException.class, () -> useCase.checkout(new CheckoutRequestDto("PRO", TitularSuscripcion.USUARIO)));
        verify(stripe, never()).crearCheckout(any());
    }

    @Test
    void companyCheckoutRequiresAdministrator() {
        assertThrows(AccessDeniedException.class, () -> useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.EMPRESA)));
        verify(stripe, never()).crearCliente(anyString(), anyString(), any());
    }

    @Test
    void adminCompanyCheckoutUsesOwnCompany() {
        when(tenant.isAdministrator()).thenReturn(true);
        useCase.checkout(new CheckoutRequestDto("PRO", TitularSuscripcion.EMPRESA));
        verify(suscripciones).save(org.mockito.ArgumentMatchers.argThat(s ->
            s.getTitular() == TitularSuscripcion.EMPRESA && s.getEmpresa() == empresa && s.getUsuario() == null));
    }

    @Test
    void unknownOrUnlinkedPlanIsRejected() {
        when(planes.findByCodigo("BASIC")).thenReturn(Optional.of(plan("BASIC", 7, 1, null)));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.USUARIO)));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.checkout(new CheckoutRequestDto("GOLD", TitularSuscripcion.USUARIO)));
    }

    @Test
    void withoutStripeConfigurationPaymentsAreUnavailableButPlansAreListed() {
        when(stripe.isConfigured()).thenReturn(false);
        when(planes.findByActivoTrueOrderByNivelAsc()).thenReturn(List.of(basic, pro));
        assertThrows(PagosNoConfiguradosException.class, () -> useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.USUARIO)));
        assertThrows(PagosNoConfiguradosException.class, () -> useCase.portal(TitularSuscripcion.USUARIO));
        var listed = useCase.planes();
        assertEquals(List.of("BASIC", "PRO"), listed.stream().map(p -> p.codigo()).toList());
        assertFalse(listed.get(0).disponible());
        assertEquals(new java.math.BigDecimal("9.00"), listed.get(0).precioMensual());
        assertEquals(new java.math.BigDecimal("19.00"), listed.get(1).precioMensual());
    }

    @Test
    void effectivePlanPrefersHigherActiveLevelAndIgnoresExpired() {
        Suscripcion propia = Suscripcion.paraUsuario(usuario, "cus_u");
        propia.actualizarDesdeStripe("sub_u", basic, "trialing", Instant.now().plus(3, ChronoUnit.DAYS), Instant.now().plus(3, ChronoUnit.DAYS), false);
        Suscripcion deEmpresa = Suscripcion.paraEmpresa(empresa, "cus_e");
        deEmpresa.actualizarDesdeStripe("sub_e", pro, "active", null, Instant.now().plus(20, ChronoUnit.DAYS), false);
        when(suscripciones.findByUsuarioId(7L)).thenReturn(Optional.of(propia));
        when(suscripciones.findByEmpresaId(10L)).thenReturn(Optional.of(deEmpresa));
        assertEquals("PRO", consulta.planEfectivo(usuario).orElseThrow().getPlan().getCodigo());

        deEmpresa.actualizarDesdeStripe("sub_e", pro, "active", null, Instant.now().minus(1, ChronoUnit.DAYS), false);
        assertEquals("BASIC", consulta.planEfectivo(usuario).orElseThrow().getPlan().getCodigo());

        propia.actualizarDesdeStripe("sub_u", basic, "past_due", null, Instant.now().plus(3, ChronoUnit.DAYS), false);
        assertTrue(consulta.planEfectivo(usuario).isEmpty());
    }

    @Test
    void blankPriceIdsDisablePlansAndRejectCheckoutBeforeSideEffects() {
        basic.setStripePriceId("   ");
        when(planes.findByActivoTrueOrderByNivelAsc()).thenReturn(List.of(basic, pro));
        assertFalse(useCase.planes().get(0).disponible());
        assertTrue(useCase.planes().get(1).disponible());
        assertThrows(SolicitudInvalidaException.class,
            () -> useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.USUARIO)));
        org.mockito.Mockito.verifyNoInteractions(suscripciones, usuarios, tenant);
        verify(stripe, never()).crearCliente(anyString(), anyString(), any());
        verify(stripe, never()).crearCheckout(any());
    }

    @Test
    void incompleteConfigurationBlocksCheckoutAndPortalBeforeSideEffects() {
        when(stripe.isConfigured()).thenReturn(false);
        assertThrows(PagosNoConfiguradosException.class,
            () -> useCase.checkout(new CheckoutRequestDto("BASIC", TitularSuscripcion.USUARIO)));
        assertThrows(PagosNoConfiguradosException.class, () -> useCase.portal(TitularSuscripcion.USUARIO));
        org.mockito.Mockito.verifyNoInteractions(planes, suscripciones, usuarios, tenant);
        verify(stripe, never()).crearCliente(anyString(), anyString(), any());
        verify(stripe, never()).crearCheckout(any());
        verify(stripe, never()).crearPortal(anyString(), anyString());
    }

    private CheckoutStripe captureCheckout() {
        ArgumentCaptor<CheckoutStripe> captor = ArgumentCaptor.forClass(CheckoutStripe.class);
        verify(stripe).crearCheckout(captor.capture());
        return captor.getValue();
    }

    private static PlanSuscripcion plan(String codigo, int diasPrueba, int nivel, String priceId) {
        long precioCentavos = "BASIC".equals(codigo) ? 900 : "PRO".equals(codigo) ? 1900 : 0;
        PlanSuscripcion plan = new PlanSuscripcion(codigo, codigo, precioCentavos, "usd", diasPrueba, nivel, List.of("Beneficio"));
        plan.setStripePriceId(priceId);
        return plan;
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
