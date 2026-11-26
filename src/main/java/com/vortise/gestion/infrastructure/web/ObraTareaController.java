package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import com.vortise.gestion.application.dto.ObraTareaDto;
import com.vortise.gestion.application.service.ObraTareaService;
import com.vortise.gestion.application.service.PrevistoRealService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/obra-tareas")
public class ObraTareaController {
    private final ObraTareaService obraTareaService;
    private final PrevistoRealService comparison;

    public ObraTareaController(ObraTareaService obraTareaService, PrevistoRealService comparison) {
        this.obraTareaService = obraTareaService;
        this.comparison = comparison;
    }

    @GetMapping("/indicadores")
    public PrevistoRealService.Summary indicadores(@PathVariable Long proyectoId) {
        return comparison.execute(proyectoId);
    }

    @GetMapping
    public ResponseEntity<List<ObraTareaDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(obraTareaService.listar(proyectoId));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<ObraTareaDto> create(@PathVariable Long proyectoId, @Valid @RequestBody ObraTareaCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(obraTareaService.create(proyectoId, dto));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<ObraTareaDto> update(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody ObraTareaCreateDto dto) {
        return ResponseEntity.ok(obraTareaService.update(proyectoId, id, dto));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.GERENTE)
    public ResponseEntity<Void> delete(@PathVariable Long proyectoId, @PathVariable Long id) {
        obraTareaService.delete(proyectoId, id);
        return ResponseEntity.noContent().build();
    }
}
