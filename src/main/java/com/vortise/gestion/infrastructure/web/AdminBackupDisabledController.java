package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Profile("staging")
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class AdminBackupDisabledController {
    private static final String MESSAGE =
        "El backup y restore lógico solo está disponible con H2 local; está deshabilitado en staging PostgreSQL.";

    @GetMapping
    public void exportBackup() {
        throw new FeatureUnavailableException(MESSAGE);
    }

    @PostMapping(value = "/restore", consumes = "multipart/form-data")
    public void restore(@RequestParam("file") MultipartFile ignored) {
        throw new FeatureUnavailableException(MESSAGE);
    }
}
