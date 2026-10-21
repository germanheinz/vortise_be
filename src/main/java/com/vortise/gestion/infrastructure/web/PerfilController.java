package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.MeDto;
import com.vortise.gestion.application.usecase.PerfilUseCase;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class PerfilController {
    private final PerfilUseCase useCase;

    public PerfilController(PerfilUseCase useCase) { this.useCase = useCase; }

    @GetMapping
    public ResponseEntity<MeDto> me() { return ResponseEntity.ok(useCase.me()); }

    @PutMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> subirFoto(@RequestParam("file") MultipartFile file) throws IOException {
        useCase.subirFoto(file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto() {
        return useCase.obtenerFoto()
            .map(foto -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo().contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.datos()))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/foto")
    public ResponseEntity<Void> eliminarFoto() {
        useCase.eliminarFoto();
        return ResponseEntity.noContent().build();
    }
}
