package com.vortise.gestion.controller.pagos;

import com.vortise.gestion.infrastructure.security.Guards;
import com.vortise.gestion.application.dto.CheckoutRequestDto;
import com.vortise.gestion.application.dto.MisSuscripcionesDto;
import com.vortise.gestion.application.dto.PlanSuscripcionDto;
import com.vortise.gestion.application.dto.PortalRequestDto;
import com.vortise.gestion.application.dto.SuscripcionResumenDto;
import com.vortise.gestion.application.dto.UrlDto;
import com.vortise.gestion.service.pagos.SuscripcionConsultaService;
import com.vortise.gestion.service.pagos.SuscripcionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SuscripcionController {
    private final SuscripcionService useCase;
    private final SuscripcionConsultaService consulta;

    public SuscripcionController(SuscripcionService useCase, SuscripcionConsultaService consulta) {
        this.useCase = useCase;
        this.consulta = consulta;
    }

    @GetMapping("/suscripciones/planes")
    public ResponseEntity<List<PlanSuscripcionDto>> planes() { return ResponseEntity.ok(useCase.planes()); }

    @PreAuthorize("!hasRole('COLABORADOR')")
    @GetMapping("/suscripciones/mias")
    public ResponseEntity<MisSuscripcionesDto> mias() { return ResponseEntity.ok(useCase.mias()); }

    @PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.SOLO_ADMINISTRADOR)
    @PostMapping("/suscripciones/checkout")
    public ResponseEntity<UrlDto> checkout(@Valid @RequestBody CheckoutRequestDto request) { return ResponseEntity.ok(useCase.checkout(request)); }

    @PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.SOLO_ADMINISTRADOR)
    @PostMapping("/suscripciones/portal")
    public ResponseEntity<UrlDto> portal(@Valid @RequestBody PortalRequestDto request) { return ResponseEntity.ok(useCase.portal(request.titular())); }

    /** Company ownership is enforced by TenantAccessInterceptor on /api/empresas/**. */
    @GetMapping("/empresas/{empresaId}/suscripcion")
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<SuscripcionResumenDto> empresa(@PathVariable Long empresaId) {
        return consulta.resumenEmpresa(empresaId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
