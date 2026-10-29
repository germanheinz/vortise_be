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

import com.vortise.gestion.application.dto.PaginaDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.ProyectoUpdateDto;
import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.usecase.CreateProyectoUseCase;
import com.vortise.gestion.application.usecase.DeleteProyectoUseCase;
import com.vortise.gestion.application.usecase.GetProyectoByIdUseCase;
import com.vortise.gestion.application.usecase.GetProyectoPaginaUseCase;
import com.vortise.gestion.application.usecase.GetProyectosUseCase;
import com.vortise.gestion.application.usecase.UpdateProyectoUseCase;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/proyectos"})
public class ProyectoController {
    private final GetProyectoPaginaUseCase pagina;
    private final GetProyectosUseCase getProyectosUseCase;
    private final GetProyectoByIdUseCase getProyectoByIdUseCase;
    private final CreateProyectoUseCase createProyectoUseCase;
    private final UpdateProyectoUseCase updateProyectoUseCase;
    private final DeleteProyectoUseCase deleteProyectoUseCase;
    private final DataSeeder dataSeeder;
    private final ProductionFeaturePolicy featurePolicy;

    @GetMapping
    public ResponseEntity<List<ProyectoDto>> getAll() {
        return ResponseEntity.ok(this.getProyectosUseCase.execute());
    }

    @GetMapping("/pagina")
    public ResponseEntity<PaginaDto<com.vortise.gestion.application.dto.ProyectoDto>> getPagina(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(pagina.execute(null, q, page, size));
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<ProyectoDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(this.getProyectoByIdUseCase.execute(id));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("!hasRole('COLABORADOR')")
    public ResponseEntity<ProyectoDto> create(@Valid @RequestBody ProyectoCreateDto dto) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.createProyectoUseCase.execute(dto));
    }

    @PutMapping(value="/{id}")
    public ResponseEntity<ProyectoDto> update(@PathVariable Long id, @Valid @RequestBody ProyectoUpdateDto dto) {
        return ResponseEntity.ok(this.updateProyectoUseCase.execute(id, dto));
    }

    @DeleteMapping(value="/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.deleteProyectoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value="/reset-demo")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> resetDemo() {
        if (!featurePolicy.demoResetEnabled()) {
            throw new FeatureUnavailableException("El reset demo está deshabilitado hasta confirmar un entorno de datos sintéticos aislado.");
        }
        this.dataSeeder.resetDemoData();
        return ResponseEntity.ok("Demo seed reset successfully");
    }

    public ProyectoController(
        GetProyectosUseCase getProyectosUseCase,
        GetProyectoByIdUseCase getProyectoByIdUseCase,
        CreateProyectoUseCase createProyectoUseCase,
        UpdateProyectoUseCase updateProyectoUseCase,
        DeleteProyectoUseCase deleteProyectoUseCase,
        DataSeeder dataSeeder,
        ProductionFeaturePolicy featurePolicy,
        GetProyectoPaginaUseCase pagina
    ) {
        this.pagina = pagina;
        this.getProyectosUseCase = getProyectosUseCase;
        this.getProyectoByIdUseCase = getProyectoByIdUseCase;
        this.createProyectoUseCase = createProyectoUseCase;
        this.updateProyectoUseCase = updateProyectoUseCase;
        this.deleteProyectoUseCase = deleteProyectoUseCase;
        this.dataSeeder = dataSeeder;
        this.featurePolicy = featurePolicy;
    }
}
