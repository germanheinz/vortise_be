package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.application.usecase.PlantaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/plantas")
public class PlantaController {
    private final PlantaUseCase useCase;
    public PlantaController(PlantaUseCase useCase) { this.useCase = useCase; }
    @GetMapping
    public ResponseEntity<List<PlantaDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(useCase.getAll(proyectoId));
    }
    @PostMapping
    public ResponseEntity<PlantaDto> create(@PathVariable Long proyectoId, @Valid @RequestBody PlantaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(proyectoId, dto));
    }
    @PutMapping("/{id}")
    public ResponseEntity<PlantaDto> update(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody PlantaDto dto) {
        return ResponseEntity.ok(useCase.update(proyectoId, id, dto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long proyectoId, @PathVariable Long id) {
        useCase.delete(proyectoId, id);
        return ResponseEntity.noContent().build();
    }
}
