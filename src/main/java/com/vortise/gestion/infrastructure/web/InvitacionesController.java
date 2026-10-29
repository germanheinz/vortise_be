package com.vortise.gestion.infrastructure.web;
import com.vortise.gestion.application.dto.InvitacionesDto.*;
import com.vortise.gestion.application.usecase.InvitacionesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/equipo/invitaciones")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class InvitacionesController {
    private final InvitacionesUseCase useCase;
    public InvitacionesController(InvitacionesUseCase useCase) { this.useCase = useCase; }
    @GetMapping public Equipo listar() { return useCase.listar(); }
    @PostMapping public ResponseEntity<Creada> crear(@Valid @RequestBody Crear dto) { return ResponseEntity.status(201).body(useCase.crear(dto)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> revocar(@PathVariable Long id) { useCase.revocar(id); return ResponseEntity.noContent().build(); }
}
