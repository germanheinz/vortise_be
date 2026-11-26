package com.vortise.gestion.controller;

import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.service.PlantaService;
import com.vortise.gestion.infrastructure.security.Guards;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/plantas")
public class PlantaController {
    private final PlantaService useCase;
    public PlantaController(PlantaService useCase) { this.useCase = useCase; }
    @GetMapping
    public ResponseEntity<List<PlantaDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(useCase.getAll(proyectoId));
    }
    @PostMapping
    @PreAuthorize(Guards.GERENTE)
    public ResponseEntity<PlantaDto> create(@PathVariable Long proyectoId, @Valid @RequestBody PlantaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(proyectoId, dto));
    }
    @PutMapping("/{id}")
    @PreAuthorize(Guards.GERENTE)
    public ResponseEntity<PlantaDto> update(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody PlantaDto dto) {
        return ResponseEntity.ok(useCase.update(proyectoId, id, dto));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize(Guards.GERENTE)
    public ResponseEntity<Void> delete(@PathVariable Long proyectoId, @PathVariable Long id) {
        useCase.delete(proyectoId, id);
        return ResponseEntity.noContent().build();
    }
}
