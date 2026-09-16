package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.UsuarioEmpresaDto;
import com.vortise.gestion.application.usecase.UsuarioEmpresaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas/{empresaId}/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioEmpresaController {
    private final UsuarioEmpresaUseCase useCase;
    public UsuarioEmpresaController(UsuarioEmpresaUseCase useCase) { this.useCase = useCase; }
    @GetMapping public ResponseEntity<List<UsuarioEmpresaDto>> getAll(@PathVariable Long empresaId) { return ResponseEntity.ok(useCase.getAll(empresaId)); }
    @PostMapping public ResponseEntity<UsuarioEmpresaDto> create(@PathVariable Long empresaId, @Valid @RequestBody UsuarioEmpresaDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(empresaId, dto)); }
    @PutMapping("/{id}") public ResponseEntity<UsuarioEmpresaDto> update(@PathVariable Long empresaId, @PathVariable Long id, @Valid @RequestBody UsuarioEmpresaDto dto) { return ResponseEntity.ok(useCase.update(empresaId, id, dto)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long empresaId, @PathVariable Long id) { useCase.delete(empresaId, id); return ResponseEntity.noContent().build(); }
}
