package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.RestriccionComentarioCreateDto;
import com.vortise.gestion.application.dto.RestriccionComentarioDto;
import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.dto.RestriccionUpdateDto;
import com.vortise.gestion.application.usecase.ConsultarRestriccionesObraUseCase;
import com.vortise.gestion.application.usecase.ConsultarRestriccionesObraUseCase.Filtro;
import com.vortise.gestion.application.usecase.ConsultarRestriccionesObraUseCase.Respuesta;
import com.vortise.gestion.application.usecase.CreateRestriccionComentarioUseCase;
import com.vortise.gestion.application.usecase.CreateRestriccionUseCase;
import com.vortise.gestion.application.usecase.CrearRestriccionChatUseCase;
import com.vortise.gestion.application.usecase.DeleteRestriccionUseCase;
import com.vortise.gestion.application.usecase.GetRestriccionByIdUseCase;
import com.vortise.gestion.application.usecase.GetRestriccionComentariosUseCase;
import com.vortise.gestion.application.usecase.GetRestriccionesUseCase;
import com.vortise.gestion.application.usecase.UpdateRestriccionUseCase;
import com.vortise.gestion.infrastructure.security.Guards;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestriccionController {
    private final CrearRestriccionChatUseCase crearRestriccionChatUseCase;
    private final ConsultarRestriccionesObraUseCase consultarRestriccionesObraUseCase;
    private final GetRestriccionesUseCase getRestriccionesUseCase;
    private final CreateRestriccionUseCase createRestriccionUseCase;
    private final GetRestriccionByIdUseCase getRestriccionByIdUseCase;
    private final UpdateRestriccionUseCase updateRestriccionUseCase;
    private final DeleteRestriccionUseCase deleteRestriccionUseCase;
    private final GetRestriccionComentariosUseCase getRestriccionComentariosUseCase;
    private final CreateRestriccionComentarioUseCase createRestriccionComentarioUseCase;

    public RestriccionController(
        CrearRestriccionChatUseCase crearRestriccionChatUseCase,
        ConsultarRestriccionesObraUseCase consultarRestriccionesObraUseCase,
        GetRestriccionesUseCase getRestriccionesUseCase,
        CreateRestriccionUseCase createRestriccionUseCase,
        GetRestriccionByIdUseCase getRestriccionByIdUseCase,
        UpdateRestriccionUseCase updateRestriccionUseCase,
        DeleteRestriccionUseCase deleteRestriccionUseCase,
        GetRestriccionComentariosUseCase getRestriccionComentariosUseCase,
        CreateRestriccionComentarioUseCase createRestriccionComentarioUseCase
    ) {
        this.crearRestriccionChatUseCase = crearRestriccionChatUseCase;
        this.consultarRestriccionesObraUseCase = consultarRestriccionesObraUseCase;
        this.getRestriccionesUseCase = getRestriccionesUseCase;
        this.createRestriccionUseCase = createRestriccionUseCase;
        this.getRestriccionByIdUseCase = getRestriccionByIdUseCase;
        this.updateRestriccionUseCase = updateRestriccionUseCase;
        this.deleteRestriccionUseCase = deleteRestriccionUseCase;
        this.getRestriccionComentariosUseCase = getRestriccionComentariosUseCase;
        this.createRestriccionComentarioUseCase = createRestriccionComentarioUseCase;
    }

    @GetMapping("/proyectos/{proyectoId}/restricciones")
    public ResponseEntity<List<RestriccionDto>> getAll(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(getRestriccionesUseCase.execute(proyectoId));
    }

    @GetMapping("/proyectos/{proyectoId}/chat/restricciones")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public Respuesta consultarParaGerencia(@PathVariable Long proyectoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate corte,
            @RequestParam(defaultValue = "ABIERTAS") Filtro filtro,
            @RequestParam(required = false) String prioridad) {
        return consultarRestriccionesObraUseCase.execute(proyectoId, corte, filtro, prioridad);
    }

    @PostMapping("/proyectos/{proyectoId}/chat/restricciones")
    @PreAuthorize(Guards.GERENCIA_CHAT)
    public ResponseEntity<CrearRestriccionChatUseCase.Resultado> crearParaGerencia(@PathVariable Long proyectoId,
            @RequestHeader(value = "Idempotency-Key", required = false) String clave,
            @Valid @RequestBody CrearRestriccionChatUseCase.Solicitud solicitud) {
        var resultado = crearRestriccionChatUseCase.execute(proyectoId, clave, solicitud);
        return ResponseEntity.status(resultado.repetida() ? HttpStatus.OK : HttpStatus.CREATED).body(resultado);
    }

    @GetMapping("/restricciones")
    public ResponseEntity<List<RestriccionDto>> getAllRestricciones() {
        return ResponseEntity.ok(getRestriccionesUseCase.executeAll());
    }

    @GetMapping("/restricciones/{id}")
    public ResponseEntity<RestriccionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getRestriccionByIdUseCase.execute(id));
    }

    @PostMapping("/proyectos/{proyectoId}/restricciones")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<RestriccionDto> create(@PathVariable Long proyectoId, @Valid @RequestBody RestriccionCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createRestriccionUseCase.execute(proyectoId, dto));
    }

    // A JEFE_DE_OBRA may only release a restriction once; UpdateRestriccionUseCase enforces it.
    @PatchMapping("/restricciones/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<RestriccionDto> update(@PathVariable Long id, @RequestBody RestriccionUpdateDto dto) {
        return ResponseEntity.ok(updateRestriccionUseCase.execute(id, dto));
    }

    @DeleteMapping("/restricciones/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.GERENTE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteRestriccionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restricciones/{id}/comentarios")
    public ResponseEntity<List<RestriccionComentarioDto>> getComentarios(@PathVariable Long id) {
        return ResponseEntity.ok(getRestriccionComentariosUseCase.execute(id));
    }

    @PostMapping("/restricciones/{id}/comentarios")
    @org.springframework.security.access.prepost.PreAuthorize(com.vortise.gestion.infrastructure.security.Guards.OBRA)
    public ResponseEntity<RestriccionComentarioDto> createComentario(@PathVariable Long id, @Valid @RequestBody RestriccionComentarioCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createRestriccionComentarioUseCase.execute(id, dto));
    }
}