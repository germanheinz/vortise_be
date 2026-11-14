package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ReporteBorradorImagenesDto;
import com.vortise.gestion.application.dto.ReporteDetalleDto;
import com.vortise.gestion.application.dto.ReporteEmitirDto;
import com.vortise.gestion.application.dto.ReporteImagenDto;
import com.vortise.gestion.application.dto.ReporteImagenLeyendaDto;
import com.vortise.gestion.application.dto.ReporteResumenDto;
import com.vortise.gestion.application.usecase.ReporteObraUseCase;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

/** Tenant access for {@code /api/proyectos/{id}/**} is enforced by TenantAccessInterceptor. */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/reportes")
public class ReporteObraController {
    private final ReporteObraUseCase useCase;

    public ReporteObraController(ReporteObraUseCase useCase) { this.useCase = useCase; }

    @GetMapping("/imagenes")
    public ReporteBorradorImagenesDto borrador(@PathVariable Long proyectoId) {
        return useCase.borrador(proyectoId);
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteImagenDto> subirImagen(@PathVariable Long proyectoId, @RequestParam("file") MultipartFile file,
            @RequestParam(value = "leyenda", required = false) String leyenda,
            @RequestParam(value = "fecha", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
            @RequestParam(value = "rubroId", required = false) Long rubroId,
            @RequestParam(value = "nivelId", required = false) Long nivelId) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.subirImagen(proyectoId, file.getBytes(), leyenda, fecha, rubroId, nivelId));
    }

    @PutMapping("/imagenes/{imagenId}")
    public ReporteImagenDto actualizarLeyenda(@PathVariable Long proyectoId, @PathVariable Long imagenId,
            @Valid @RequestBody ReporteImagenLeyendaDto dto) {
        return useCase.actualizarLeyenda(proyectoId, imagenId, dto.leyenda());
    }

    @DeleteMapping("/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long proyectoId, @PathVariable Long imagenId) {
        useCase.eliminarImagen(proyectoId, imagenId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<ReporteDetalleDto> emitir(@PathVariable Long proyectoId, @Valid @RequestBody ReporteEmitirDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.emitir(proyectoId, dto));
    }

    @GetMapping
    public List<ReporteResumenDto> listar(@PathVariable Long proyectoId) {
        return useCase.listar(proyectoId);
    }

    @GetMapping("/{reporteId}")
    public ReporteDetalleDto detalle(@PathVariable Long proyectoId, @PathVariable Long reporteId) {
        return useCase.detalle(proyectoId, reporteId);
    }
}
