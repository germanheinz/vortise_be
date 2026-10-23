package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.SuperAdminDtos.DashboardDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.EmpresaDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.PageDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.PagoDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.SuscripcionDto;
import com.vortise.gestion.application.dto.SuperAdminDtos.UsuarioDto;
import com.vortise.gestion.application.usecase.SuperAdminConsultaUseCase;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {
    private final SuperAdminConsultaUseCase consulta;

    public SuperAdminController(SuperAdminConsultaUseCase consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return consulta.dashboard();
    }

    @GetMapping("/empresas")
    public PageDto<EmpresaDto> empresas(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String q) {
        return consulta.empresas(page, size, q);
    }

    @GetMapping("/usuarios")
    public PageDto<UsuarioDto> usuarios(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String q,
            @RequestParam(required = false) Long empresaId, @RequestParam(required = false) Boolean activo) {
        return consulta.usuarios(page, size, q, empresaId, activo);
    }

    @GetMapping("/suscripciones")
    public PageDto<SuscripcionDto> suscripciones(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long empresaId) {
        return consulta.suscripciones(page, size, estado, empresaId);
    }

    @GetMapping("/pagos")
    public PageDto<PagoDto> pagos(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String estado,
            @RequestParam(required = false) LocalDate desde, @RequestParam(required = false) LocalDate hasta) {
        return consulta.pagos(page, size, estado, desde, hasta);
    }
}
