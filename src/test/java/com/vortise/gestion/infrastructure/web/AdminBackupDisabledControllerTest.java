package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.infrastructure.backup.H2LogicalBackupService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class AdminBackupDisabledControllerTest {
    @Test
    void backupRoutesAreLocalOnlyAndHaveAnExplicitDeployedReplacement() {
        Profile h2Controller = AdminBackupController.class.getAnnotation(Profile.class);
        Profile stagingController = AdminBackupDisabledController.class.getAnnotation(Profile.class);
        Profile h2Service = H2LogicalBackupService.class.getAnnotation(Profile.class);

        assertNotNull(h2Controller);
        assertNotNull(stagingController);
        assertNotNull(h2Service);
        assertEquals("!staging & !prod & !production & !e2e-local", h2Controller.value()[0]);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"staging", "prod", "production", "e2e-local"}, stagingController.value());
        assertEquals("!staging & !prod & !production & !e2e-local", h2Service.value()[0]);
    }

    @Test
    void deployedExportAndRestoreFailWithClearUnavailableErrors() {
        AdminBackupDisabledController controller = new AdminBackupDisabledController();

        FeatureUnavailableException export = assertThrows(FeatureUnavailableException.class, controller::exportBackup);
        FeatureUnavailableException restore = assertThrows(FeatureUnavailableException.class, () -> controller.restore(null));

        assertEquals(export.getMessage(), restore.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(export.getMessage().contains("H2 local"));
        org.junit.jupiter.api.Assertions.assertTrue(export.getMessage().contains("entornos desplegados con PostgreSQL"));
        var response = new GlobalExceptionHandler().handleFeatureUnavailable(export);
        assertEquals(503, response.getStatusCode().value());
        assertEquals(export.getMessage(), java.util.Objects.requireNonNull(response.getBody()).message());
    }
}
