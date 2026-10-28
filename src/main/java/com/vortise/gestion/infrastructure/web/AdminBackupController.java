package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.infrastructure.backup.BackupValidationException;
import com.vortise.gestion.infrastructure.backup.H2LogicalBackupService;
import com.vortise.gestion.infrastructure.backup.H2LogicalBackupService.RestoreSummary;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@Profile("!staging & !prod & !production & !e2e-local")
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class AdminBackupController {
    private static final long MAX_FILE_BYTES = H2LogicalBackupService.MAX_BACKUP_BYTES;
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC);

    private final H2LogicalBackupService backupService;

    public AdminBackupController(H2LogicalBackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ByteArrayResource> exportBackup() {
        byte[] backup = backupService.exportBackup();
        String filename = "gestion-backup-" + FILE_TIMESTAMP.format(Instant.now()) + ".json";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/json"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .contentLength(backup.length)
            .body(new ByteArrayResource(backup));
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestoreResponse> restore(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            throw new BackupValidationException("El archivo está vacío o supera el máximo de 50 MiB");
        }
        try {
            RestoreSummary summary = backupService.restore(file.getBytes());
            return ResponseEntity.ok(new RestoreResponse("Respaldo restaurado correctamente", summary));
        } catch (IOException ex) {
            throw new BackupValidationException("No se pudo leer el archivo de respaldo", ex);
        }
    }

    public record RestoreResponse(String message, RestoreSummary summary) { }
}
