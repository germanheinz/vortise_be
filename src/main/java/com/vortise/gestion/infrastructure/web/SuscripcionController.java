package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.CheckoutRequestDto;
import com.vortise.gestion.application.dto.MisSuscripcionesDto;
import com.vortise.gestion.application.dto.PlanSuscripcionDto;
import com.vortise.gestion.application.dto.PortalRequestDto;
import com.vortise.gestion.application.dto.SuscripcionResumenDto;
import com.vortise.gestion.application.dto.UrlDto;
import com.vortise.gestion.application.usecase.SuscripcionConsultaService;
import com.vortise.gestion.application.usecase.SuscripcionUseCase;
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
    private final SuscripcionUseCase useCase;
    private final SuscripcionConsultaService consulta;

    public SuscripcionController(SuscripcionUseCase useCase, SuscripcionConsultaService consulta) {
        this.useCase = useCase;
        this.consulta = consulta;
    }

    @GetMapping("/suscripciones/planes")
    public ResponseEntity<List<PlanSuscripcionDto>> planes() { return ResponseEntity.ok(useCase.planes()); }

    @GetMapping("/suscripciones/mias")
    public ResponseEntity<MisSuscripcionesDto> mias() { return ResponseEntity.ok(useCase.mias()); }

    @PostMapping("/suscripciones/checkout")
    public ResponseEntity<UrlDto> checkout(@Valid @RequestBody CheckoutRequestDto request) { return ResponseEntity.ok(useCase.checkout(request)); }

    @PostMapping("/suscripciones/portal")
    public ResponseEntity<UrlDto> portal(@Valid @RequestBody PortalRequestDto request) { return ResponseEntity.ok(useCase.portal(request.titular())); }

    /** Company ownership is enforced by TenantAccessInterceptor on /api/empresas/**. */
    @GetMapping("/empresas/{empresaId}/suscripcion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SuscripcionResumenDto> empresa(@PathVariable Long empresaId) {
        return consulta.resumenEmpresa(empresaId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
