package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.CertificadoCompletadoDto;
import com.vortise.gestion.application.dto.CertificadoCorteDto;
import com.vortise.gestion.application.dto.CertificadoCrearDto;
import com.vortise.gestion.application.dto.CertificadoDetalleDto;
import com.vortise.gestion.application.dto.CertificadoEmpresaDto;
import com.vortise.gestion.application.dto.CertificadoExcedenteDto;
import com.vortise.gestion.application.dto.CertificadoResumenDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.usecase.CertificadoObraUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tenant access for {@code /api/proyectos/{id}/**} is enforced by TenantAccessInterceptor; reading is open, writing is restricted by role. */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/certificados")
public class CertificadoObraController {
    private static final String ESCRITURA = "hasAnyRole('ADMINISTRADOR', 'JEFE_DE_OBRA')";
    private final CertificadoObraUseCase useCase;

    public CertificadoObraController(CertificadoObraUseCase useCase) { this.useCase = useCase; }

    @GetMapping("/empresas")
    public List<CertificadoEmpresaDto> empresas(@PathVariable Long proyectoId) {
        return useCase.empresas(proyectoId);
    }

    /** {@code empresa} is "propia" for the own labor or the id of a subcontractor. */
    @GetMapping
    public List<CertificadoResumenDto> listar(@PathVariable Long proyectoId, @RequestParam String empresa) {
        return useCase.listar(proyectoId, empresa(empresa));
    }

    @GetMapping("/excedentes")
    public List<CertificadoExcedenteDto> excedentes(@PathVariable Long proyectoId) {
        return useCase.excedentes(proyectoId);
    }

    @GetMapping("/{certificadoId}")
    public CertificadoDetalleDto detalle(@PathVariable Long proyectoId, @PathVariable Long certificadoId) {
        return useCase.detalle(proyectoId, certificadoId);
    }

    @PostMapping
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<CertificadoDetalleDto> crear(@PathVariable Long proyectoId, @Valid @RequestBody CertificadoCrearDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.crear(proyectoId, dto));
    }

    @PutMapping("/{certificadoId}/corte")
    @PreAuthorize(ESCRITURA)
    public CertificadoDetalleDto editarCorte(@PathVariable Long proyectoId, @PathVariable Long certificadoId, @Valid @RequestBody CertificadoCorteDto dto) {
        return useCase.editarCorte(proyectoId, certificadoId, dto.fechaCorte());
    }

    @PostMapping("/{certificadoId}/cerrar")
    @PreAuthorize(ESCRITURA)
    public CertificadoDetalleDto cerrar(@PathVariable Long proyectoId, @PathVariable Long certificadoId) {
        return useCase.cerrar(proyectoId, certificadoId);
    }

    @PutMapping("/{certificadoId}/items/{rubroId}/completado")
    @PreAuthorize(ESCRITURA)
    public CertificadoDetalleDto completado(@PathVariable Long proyectoId, @PathVariable Long certificadoId, @PathVariable Long rubroId,
            @RequestBody CertificadoCompletadoDto dto) {
        return useCase.marcarCompletado(proyectoId, certificadoId, rubroId, dto.completado());
    }

    private static Long empresa(String value) {
        if ("propia".equals(value)) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException error) {
            throw new SolicitudInvalidaException("Empresa inválida");
        }
    }
}
