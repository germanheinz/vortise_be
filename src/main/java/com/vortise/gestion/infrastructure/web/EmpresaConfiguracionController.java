package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ResponsableEmpresaDto;
import com.vortise.gestion.application.dto.UnidadMetricaEmpresaDto;
import com.vortise.gestion.application.dto.CatalogoEmpresaDto;
import com.vortise.gestion.application.dto.CalendarioLaboralEmpresaDto;
import com.vortise.gestion.application.dto.CategoriaManoObraEmpresaDto;
import com.vortise.gestion.application.usecase.EmpresaConfiguracionUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas/{empresaId}/configuracion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EmpresaConfiguracionController {
    private final EmpresaConfiguracionUseCase useCase;

    public EmpresaConfiguracionController(EmpresaConfiguracionUseCase useCase) { this.useCase = useCase; }

    @GetMapping("/responsables")
    public ResponseEntity<List<ResponsableEmpresaDto>> getResponsables(@PathVariable Long empresaId) { return ResponseEntity.ok(useCase.getResponsables(empresaId)); }
    @PostMapping("/responsables")
    public ResponseEntity<ResponsableEmpresaDto> createResponsable(@PathVariable Long empresaId, @Valid @RequestBody ResponsableEmpresaDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(useCase.saveResponsable(empresaId, dto)); }
    @PutMapping("/responsables/{id}")
    public ResponseEntity<ResponsableEmpresaDto> updateResponsable(@PathVariable Long empresaId, @PathVariable Long id, @Valid @RequestBody ResponsableEmpresaDto dto) { return ResponseEntity.ok(useCase.saveResponsable(empresaId, new ResponsableEmpresaDto(id, dto.rol(), dto.nombre(), dto.email(), dto.telefono()))); }
    @DeleteMapping("/responsables/{id}")
    public ResponseEntity<Void> deleteResponsable(@PathVariable Long empresaId, @PathVariable Long id) { useCase.deleteResponsable(empresaId, id); return ResponseEntity.noContent().build(); }

    @GetMapping("/unidades")
    public ResponseEntity<List<UnidadMetricaEmpresaDto>> getUnidades(@PathVariable Long empresaId) { return ResponseEntity.ok(useCase.getUnidades(empresaId)); }
    @PostMapping("/unidades")
    public ResponseEntity<UnidadMetricaEmpresaDto> createUnidad(@PathVariable Long empresaId, @Valid @RequestBody UnidadMetricaEmpresaDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(useCase.saveUnidad(empresaId, dto)); }
    @PutMapping("/unidades/{id}")
    public ResponseEntity<UnidadMetricaEmpresaDto> updateUnidad(@PathVariable Long empresaId, @PathVariable Long id, @Valid @RequestBody UnidadMetricaEmpresaDto dto) { return ResponseEntity.ok(useCase.saveUnidad(empresaId, new UnidadMetricaEmpresaDto(id, dto.codigo(), dto.nombre()))); }
    @DeleteMapping("/unidades/{id}")
    public ResponseEntity<Void> deleteUnidad(@PathVariable Long empresaId, @PathVariable Long id) { useCase.deleteUnidad(empresaId, id); return ResponseEntity.noContent().build(); }

    @GetMapping("/catalogos/{tipo}")
    public ResponseEntity<List<CatalogoEmpresaDto>> getCatalogo(@PathVariable Long empresaId, @PathVariable String tipo) { return ResponseEntity.ok(useCase.getCatalogo(empresaId, tipo)); }
    @PostMapping("/catalogos")
    public ResponseEntity<CatalogoEmpresaDto> createCatalogo(@PathVariable Long empresaId, @Valid @RequestBody CatalogoEmpresaDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(useCase.saveCatalogo(empresaId, dto)); }
    @PutMapping("/catalogos/{id}")
    public ResponseEntity<CatalogoEmpresaDto> updateCatalogo(@PathVariable Long empresaId, @PathVariable Long id, @Valid @RequestBody CatalogoEmpresaDto dto) { return ResponseEntity.ok(useCase.saveCatalogo(empresaId, new CatalogoEmpresaDto(id, dto.tipo(), dto.codigo(), dto.nombre()))); }
    @DeleteMapping("/catalogos/{id}")
    public ResponseEntity<Void> deleteCatalogo(@PathVariable Long empresaId, @PathVariable Long id) { useCase.deleteCatalogo(empresaId, id); return ResponseEntity.noContent().build(); }

    @GetMapping("/calendario")
    public ResponseEntity<CalendarioLaboralEmpresaDto> getCalendario(@PathVariable Long empresaId) { return ResponseEntity.ok(useCase.getCalendario(empresaId)); }
    @PutMapping("/calendario")
    public ResponseEntity<CalendarioLaboralEmpresaDto> saveCalendario(@PathVariable Long empresaId, @Valid @RequestBody CalendarioLaboralEmpresaDto dto) { return ResponseEntity.ok(useCase.saveCalendario(empresaId, dto)); }

    @GetMapping("/categorias-mano-obra")
    public ResponseEntity<List<CategoriaManoObraEmpresaDto>> getCategoriasManoObra(@PathVariable Long empresaId) { return ResponseEntity.ok(useCase.getCategoriasManoObra(empresaId)); }
    @PutMapping("/categorias-mano-obra/{id}")
    public ResponseEntity<CategoriaManoObraEmpresaDto> updateCategoriaManoObra(@PathVariable Long empresaId, @PathVariable Long id, @Valid @RequestBody CategoriaManoObraEmpresaDto dto) { return ResponseEntity.ok(useCase.saveCategoriaManoObra(empresaId, id, dto)); }
}
