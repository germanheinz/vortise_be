package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.InvitacionUsuarioDto.*;
import com.vortise.gestion.application.usecase.InvitacionUsuarioUseCase;
import com.vortise.gestion.infrastructure.security.Guards;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Administrator-only: invite people to the company with a fixed role. Company ownership is checked by TenantAccessInterceptor. */
@RestController
@RequestMapping("/api/empresas/{empresaId}/usuarios/invitaciones")
@PreAuthorize(Guards.SOLO_ADMINISTRADOR)
public class InvitacionUsuarioController {
    private final InvitacionUsuarioUseCase useCase;
    public InvitacionUsuarioController(InvitacionUsuarioUseCase useCase) { this.useCase = useCase; }
    @GetMapping public List<Resumen> listar(@PathVariable Long empresaId) { return useCase.listar(empresaId); }
    @PostMapping public ResponseEntity<Creada> crear(@PathVariable Long empresaId, @Valid @RequestBody Crear dto) {
        return ResponseEntity.status(201).header("Cache-Control", "no-store").body(useCase.crear(empresaId, dto));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> revocar(@PathVariable Long empresaId, @PathVariable Long id) {
        useCase.revocar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
