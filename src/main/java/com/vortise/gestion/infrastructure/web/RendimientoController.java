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

import com.vortise.gestion.application.dto.RendimientoDto;
import com.vortise.gestion.application.usecase.DeleteRendimientoUseCase;
import com.vortise.gestion.application.usecase.GetRendimientosUseCase;
import com.vortise.gestion.application.usecase.SaveRendimientoUseCase;
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
@RequestMapping(value={"/api/rendimientos"})
public class RendimientoController {
    private final GetRendimientosUseCase getRendimientosUseCase;
    private final SaveRendimientoUseCase saveRendimientoUseCase;
    private final DeleteRendimientoUseCase deleteRendimientoUseCase;

    @GetMapping
    public ResponseEntity<List<RendimientoDto>> getAll() {
        return ResponseEntity.ok(this.getRendimientosUseCase.execute());
    }

    @PostMapping
    public ResponseEntity<RendimientoDto> create(@Valid @RequestBody RendimientoDto dto) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.saveRendimientoUseCase.execute(dto));
    }

    @PutMapping(value={"/{id}"})
    public ResponseEntity<RendimientoDto> update(@PathVariable Long id, @Valid @RequestBody RendimientoDto dto) {
        RendimientoDto withId = new RendimientoDto(id, dto.fase(), dto.etapa(), dto.sistema(), dto.rol(), dto.hhPorM2(), dto.precioPorHh());
        return ResponseEntity.ok(this.saveRendimientoUseCase.execute(withId));
    }

    @DeleteMapping(value={"/{id}"})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.deleteRendimientoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    public RendimientoController(GetRendimientosUseCase getRendimientosUseCase, SaveRendimientoUseCase saveRendimientoUseCase, DeleteRendimientoUseCase deleteRendimientoUseCase) {
        this.getRendimientosUseCase = getRendimientosUseCase;
        this.saveRendimientoUseCase = saveRendimientoUseCase;
        this.deleteRendimientoUseCase = deleteRendimientoUseCase;
    }
}
