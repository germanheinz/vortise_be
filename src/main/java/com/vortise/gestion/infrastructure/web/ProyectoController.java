/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.usecase.CreateProyectoUseCase;
import com.vortise.gestion.application.usecase.DeleteProyectoUseCase;
import com.vortise.gestion.application.usecase.GetProyectoByIdUseCase;
import com.vortise.gestion.application.usecase.GetProyectosUseCase;
import com.vortise.gestion.application.usecase.UpdateProyectoUseCase;
import com.vortise.gestion.infrastructure.config.DataSeeder;
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
@RequestMapping(value={"/api/proyectos"})
public class ProyectoController {
    private final GetProyectosUseCase getProyectosUseCase;
    private final GetProyectoByIdUseCase getProyectoByIdUseCase;
    private final CreateProyectoUseCase createProyectoUseCase;
    private final UpdateProyectoUseCase updateProyectoUseCase;
    private final DeleteProyectoUseCase deleteProyectoUseCase;
    private final DataSeeder dataSeeder;

    @GetMapping
    public ResponseEntity<List<ProyectoDto>> getAll() {
        return ResponseEntity.ok(this.getProyectosUseCase.execute());
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<ProyectoDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(this.getProyectoByIdUseCase.execute(id));
    }

    @PostMapping
    public ResponseEntity<ProyectoDto> create(@Valid @RequestBody ProyectoCreateDto dto) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.createProyectoUseCase.execute(dto));
    }

    @PutMapping(value="/{id}")
    public ResponseEntity<ProyectoDto> update(@PathVariable Long id, @Valid @RequestBody ProyectoCreateDto dto) {
        return ResponseEntity.ok(this.updateProyectoUseCase.execute(id, dto));
    }

    @DeleteMapping(value="/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.deleteProyectoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value="/reset-demo")
    public ResponseEntity<String> resetDemo() {
        this.dataSeeder.resetDemoData();
        return ResponseEntity.ok("Demo seed reset successfully");
    }

    public ProyectoController(
        GetProyectosUseCase getProyectosUseCase,
        GetProyectoByIdUseCase getProyectoByIdUseCase,
        CreateProyectoUseCase createProyectoUseCase,
        UpdateProyectoUseCase updateProyectoUseCase,
        DeleteProyectoUseCase deleteProyectoUseCase,
        DataSeeder dataSeeder
    ) {
        this.getProyectosUseCase = getProyectosUseCase;
        this.getProyectoByIdUseCase = getProyectoByIdUseCase;
        this.createProyectoUseCase = createProyectoUseCase;
        this.updateProyectoUseCase = updateProyectoUseCase;
        this.deleteProyectoUseCase = deleteProyectoUseCase;
        this.dataSeeder = dataSeeder;
    }
}
