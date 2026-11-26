package com.vortise.gestion.controller.restriccion;

import com.vortise.gestion.application.dto.RestriccionComentarioCreateDto;
import com.vortise.gestion.application.dto.RestriccionComentarioDto;
import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.helper.RestriccionResumenGerencial.Filtro;
import com.vortise.gestion.application.helper.RestriccionResumenGerencial.Respuesta;
import com.vortise.gestion.infrastructure.security.Guards;
import com.vortise.gestion.service.restriccion.RestriccionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestriccionController {
    private final RestriccionService restricciones;
    public RestriccionController(RestriccionService restricciones) {
        this.restricciones = restricciones;
    }

    @GetMapping("/proyectos/{proyectoId}/restricciones")
    public ResponseEntity<List<RestriccionDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(restricciones.listarPorProyecto(proyectoId));
    }

    @GetMapping("/proyectos/{proyectoId}/chat/restricciones")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public Respuesta consultarParaGerencia(@PathVariable Long proyectoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate corte,
            @RequestParam(defaultValue = "ABIERTAS") Filtro filtro,
            @RequestParam(required = false) String prioridad) {
        return restricciones.consultarParaGerencia(proyectoId, corte, filtro, prioridad);
    }

    @GetMapping("/restricciones")
    public ResponseEntity<List<RestriccionDto>> getAllRestricciones() {
        return ResponseEntity.ok(restricciones.listarPorEmpresaActual());
    }

    @GetMapping("/restricciones/{id}")
    public ResponseEntity<RestriccionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(restricciones.obtener(id));
    }

    @PostMapping("/proyectos/{proyectoId}/restricciones")
    @PreAuthorize(Guards.OBRA)
    public ResponseEntity<RestriccionDto> create(@PathVariable Long proyectoId, @Valid @RequestBody RestriccionCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restricciones.crear(proyectoId, dto));
    }

    // A JEFE_DE_OBRA may only release a restriction once; RestriccionService enforces it.
    @PatchMapping("/restricciones/{id}")
    @PreAuthorize(Guards.OBRA)
    public ResponseEntity<RestriccionDto> update(@PathVariable Long id, @RequestBody RestriccionUpdateDto dto) {
        return ResponseEntity.ok(restricciones.actualizar(id, dto));
    }

    @DeleteMapping("/restricciones/{id}")
    @PreAuthorize(Guards.GERENTE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        restricciones.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restricciones/{id}/comentarios")
    public ResponseEntity<List<RestriccionComentarioDto>> getComentarios(@PathVariable Long id) {
        return ResponseEntity.ok(restricciones.listarComentarios(id));
    }

    @PostMapping("/restricciones/{id}/comentarios")
    @PreAuthorize(Guards.OBRA)
    public ResponseEntity<RestriccionComentarioDto> createComentario(@PathVariable Long id, @Valid @RequestBody RestriccionComentarioCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restricciones.crearComentario(id, dto));
    }
}