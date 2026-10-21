package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CheckoutRequestDto;
import com.vortise.gestion.application.dto.MisSuscripcionesDto;
import com.vortise.gestion.application.dto.PlanSuscripcionDto;
import com.vortise.gestion.application.dto.UrlDto;
import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.TitularSuscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuscripcionUseCase {
    private final PlanSuscripcionRepository planes;
    private final SuscripcionRepository suscripciones;
    private final UsuarioEmpresaRepository usuarios;
    private final SuscripcionConsultaService consulta;
    private final CurrentTenantService tenant;
    private final StripeGateway stripe;
    private final String frontendUrl;

    public SuscripcionUseCase(PlanSuscripcionRepository planes, SuscripcionRepository suscripciones, UsuarioEmpresaRepository usuarios,
            SuscripcionConsultaService consulta, CurrentTenantService tenant, StripeGateway stripe,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.planes = planes; this.suscripciones = suscripciones; this.usuarios = usuarios; this.consulta = consulta;
        this.tenant = tenant; this.stripe = stripe;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Transactional(readOnly = true)
    public List<PlanSuscripcionDto> planes() {
        boolean configurado = stripe.isConfigured();
        return planes.findByActivoTrueOrderByNivelAsc().stream().map(plan -> new PlanSuscripcionDto(
            plan.getCodigo(), plan.getNombre(), BigDecimal.valueOf(plan.getPrecioMensualCentavos(), 2), plan.getMoneda(),
            plan.getDiasPrueba(), plan.getBeneficios(), configurado && plan.getStripePriceId() != null)).toList();
    }

    @Transactional(readOnly = true)
    public MisSuscripcionesDto mias() {
        UsuarioEmpresa usuario = usuarioActual();
        return new MisSuscripcionesDto(consulta.resumenPropio(usuario.getId()).orElse(null),
            consulta.resumenEmpresa(usuario.getEmpresa().getId()).orElse(null));
    }

    @Transactional
    public UrlDto checkout(CheckoutRequestDto request) {
        if (!stripe.isConfigured()) throw new PagosNoConfiguradosException();
        PlanSuscripcion plan = planes.findByCodigo(request.planCodigo())
            .filter(PlanSuscripcion::isActivo)
            .filter(p -> p.getStripePriceId() != null)
            .orElseThrow(() -> new SolicitudInvalidaException("Plan no disponible"));
        UsuarioEmpresa usuario = usuarioActual();
        requireTitularPermitido(request.titular());

        Suscripcion suscripcion = buscar(request.titular(), usuario).orElseGet(() -> crear(request.titular(), usuario));
        if (suscripcion.isActiva(Instant.now())) {
            throw new IllegalStateException("Ya existe una suscripción activa. Podés gestionarla desde el portal de pagos.");
        }
        Integer diasPrueba = plan.getDiasPrueba() > 0 && !suscripcion.isPruebaUsada() ? plan.getDiasPrueba() : null;
        String url = stripe.crearCheckout(new StripeGateway.CheckoutStripe(
            suscripcion.getStripeCustomerId(), plan.getStripePriceId(), diasPrueba,
            frontendUrl + "/planes?estado=exito", frontendUrl + "/planes?estado=cancelado",
            String.valueOf(suscripcion.getId()),
            Map.of("suscripcionId", String.valueOf(suscripcion.getId()), "titular", request.titular().name(), "planCodigo", plan.getCodigo())));
        return new UrlDto(url);
    }

    @Transactional(readOnly = true)
    public UrlDto portal(TitularSuscripcion titular) {
        if (!stripe.isConfigured()) throw new PagosNoConfiguradosException();
        UsuarioEmpresa usuario = usuarioActual();
        requireTitularPermitido(titular);
        Suscripcion suscripcion = buscar(titular, usuario)
            .orElseThrow(() -> new SolicitudInvalidaException("No hay una suscripción para gestionar"));
        return new UrlDto(stripe.crearPortal(suscripcion.getStripeCustomerId(), frontendUrl + "/planes"));
    }

    private void requireTitularPermitido(TitularSuscripcion titular) {
        if (titular == TitularSuscripcion.EMPRESA && !tenant.isAdministrator()) {
            throw new AccessDeniedException("Solo un administrador puede gestionar la suscripción de la empresa");
        }
    }

    private Optional<Suscripcion> buscar(TitularSuscripcion titular, UsuarioEmpresa usuario) {
        return titular == TitularSuscripcion.EMPRESA
            ? suscripciones.findByEmpresaId(usuario.getEmpresa().getId())
            : suscripciones.findByUsuarioId(usuario.getId());
    }

    private Suscripcion crear(TitularSuscripcion titular, UsuarioEmpresa usuario) {
        if (titular == TitularSuscripcion.EMPRESA) {
            String customerId = stripe.crearCliente(usuario.getEmail(), usuario.getEmpresa().getNombre(),
                Map.of("titular", titular.name(), "empresaId", String.valueOf(usuario.getEmpresa().getId())));
            return suscripciones.save(Suscripcion.paraEmpresa(usuario.getEmpresa(), customerId));
        }
        String customerId = stripe.crearCliente(usuario.getEmail(), usuario.getNombre(),
            Map.of("titular", titular.name(), "usuarioId", String.valueOf(usuario.getId())));
        return suscripciones.save(Suscripcion.paraUsuario(usuario, customerId));
    }

    private UsuarioEmpresa usuarioActual() {
        return usuarios.findById(tenant.usuarioId())
            .filter(UsuarioEmpresa::isActivo)
            .orElseThrow(() -> new AccessDeniedException("Usuario no disponible"));
    }
}
