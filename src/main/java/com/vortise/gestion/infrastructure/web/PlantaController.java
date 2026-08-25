/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.PlantaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
@RequestMapping(value={"/api/proyectos/{proyectoId}/plantas"})
public class PlantaController {
    private final PlantaRepository plantaRepository;
    private final ProyectoRepository proyectoRepository;

    private PlantaDto toDto(Planta p) {
        return new PlantaDto(p.getId(), p.getProyecto().getId(), p.getNivel(), p.getSuperficie(), p.getSupCubierta(), p.getSupDescubierta(), p.getSupCalculada(), p.getCoefPlantaTipo());
    }

    @GetMapping
    public ResponseEntity<List<PlantaDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(this.plantaRepository.findByProyectoId(proyectoId).stream().map(this::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<PlantaDto> create(@PathVariable Long proyectoId, @Valid @RequestBody PlantaDto dto) {
        Proyecto proyecto = this.proyectoRepository.findById(proyectoId).orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Planta planta = Planta.builder().proyecto(proyecto).nivel(dto.nivel()).superficie(dto.superficie()).supCubierta(dto.supCubierta()).supDescubierta(dto.supDescubierta()).supCalculada(dto.supCalculada()).coefPlantaTipo(dto.coefPlantaTipo()).build();
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.toDto(this.plantaRepository.save(planta)));
    }

    @PutMapping(value={"/{id}"})
    public ResponseEntity<PlantaDto> update(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody PlantaDto dto) {
        Planta planta = this.plantaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Planta no encontrada"));
        planta.setNivel(dto.nivel());
        planta.setSuperficie(dto.superficie());
        planta.setSupCubierta(dto.supCubierta());
        planta.setSupDescubierta(dto.supDescubierta());
        planta.setSupCalculada(dto.supCalculada());
        planta.setCoefPlantaTipo(dto.coefPlantaTipo());
        return ResponseEntity.ok(this.toDto(this.plantaRepository.save(planta)));
    }

    @DeleteMapping(value={"/{id}"})
    public ResponseEntity<Void> delete(@PathVariable Long proyectoId, @PathVariable Long id) {
        this.plantaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public PlantaController(PlantaRepository plantaRepository, ProyectoRepository proyectoRepository) {
        this.plantaRepository = plantaRepository;
        this.proyectoRepository = proyectoRepository;
    }
}
