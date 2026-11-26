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
package com.vortise.gestion.controller;

import com.vortise.gestion.application.dto.PaginaDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoDto;
import com.vortise.gestion.application.dto.ProyectoUpdateDto;
import com.vortise.gestion.service.ProyectoService;
import com.vortise.gestion.service.ProyectoEdicionService;
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
    private final ProyectoService proyectos;
    private final ProyectoEdicionService edicionProyecto;

    @GetMapping
    public ResponseEntity<List<ProyectoDto>> getAll() {
        return ResponseEntity.ok(proyectos.listarDeEmpresaActual());
    }

    @GetMapping("/pagina")
    public ResponseEntity<PaginaDto<com.vortise.gestion.application.dto.ProyectoDto>> getPagina(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(proyectos.paginar(null, q, page, size));
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<ProyectoDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(proyectos.obtener(id));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<ProyectoDto> create(@Valid @RequestBody ProyectoCreateDto dto) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(proyectos.crear(dto));
    }

    @PutMapping(value="/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.GERENTE)
    public ResponseEntity<ProyectoDto> update(@PathVariable Long id, @Valid @RequestBody ProyectoUpdateDto dto) {
        return ResponseEntity.ok(this.edicionProyecto.execute(id, dto));
    }

    @DeleteMapping(value="/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.GERENTE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        proyectos.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    public ProyectoController(
        ProyectoEdicionService edicionProyecto,
        ProyectoService proyectos
    ) {
        this.proyectos = proyectos;
        this.edicionProyecto = edicionProyecto;
    }
}
