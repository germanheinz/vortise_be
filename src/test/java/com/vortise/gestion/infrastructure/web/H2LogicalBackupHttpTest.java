package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.infrastructure.backup.BackupValidationException;
import com.vortise.gestion.infrastructure.backup.H2LogicalBackupService;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("planta-authorization-test")
class H2LogicalBackupHttpTest {
    private static final String DATABASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String JDBC_URL = DATABASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String JWT_SECRET = Base64.getEncoder().encodeToString(new byte[64]);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private H2LogicalBackupService backupService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EmpresaRepository empresas;

    @SpyBean private JdbcTemplate observedJdbc;
    @MockBean private com.vortise.gestion.infrastructure.config.DataSeeder dataSeeder;
    @MockBean private com.vortise.gestion.infrastructure.config.UsuarioDemoSeeder usuarioDemoSeeder;
    @MockBean private com.vortise.gestion.infrastructure.config.SuperAdminSeeder superAdminSeeder;
    @MockBean private com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder planSuscripcionSeeder;

    @DynamicPropertySource
    static void isolatedProperties(DynamicPropertyRegistry registry) {
        registry.add("app.test-isolation.datasource-url", () -> JDBC_URL);
        registry.add("app.test-isolation.jwt-secret", () -> JWT_SECRET);
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-expiration-ms", () -> 3_600_000L);
    }

    @BeforeEach
    void clearIsolatedDatabase() throws Exception {
        JsonNode empty = objectMapper.readTree(backupService.exportBackup());
        for (JsonNode table : empty.path("tables")) ((com.fasterxml.jackson.databind.node.ObjectNode) table).set("rows", objectMapper.createArrayNode());
        backupService.restore(objectMapper.writeValueAsBytes(empty));
    }

    @Test
    void downloadsOnlyForSuperAdminAndRestoresRowsInForeignKeyOrderWithExactValues() throws Exception {
        insertFixtureRows();
        byte[] snapshot = backupService.exportBackup();
        JsonNode json = objectMapper.readTree(snapshot);
        assertEquals(1, json.path("version").intValue());
        assertNotNull(json.path("schemaFingerprint").textValue());
        assertEquals(25, json.path("tables").size());
        assertEquals(1, table(json, "REGISTROS_HORAS").path("rows").size());
        assertEquals(1, table(json, "EMPRESAS").path("rows").size());

        mockMvc.perform(get("/api/admin/backup"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/backup").header(HttpHeaders.AUTHORIZATION, bearer(false)))
            .andExpect(status().isForbidden());
        var exportResponse = mockMvc.perform(get("/api/admin/backup").header(HttpHeaders.AUTHORIZATION, bearer(true)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andReturn();
        String disposition = exportResponse.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("gestion-backup-"));

        jdbc.update("UPDATE empresas SET nombre = ? WHERE id = ?", "Changed after export", 77L);
        jdbc.update("DELETE FROM registros_horas WHERE id = ?", 120L);
        MockMultipartFile file = new MockMultipartFile("file", "snapshot.json", "application/json", snapshot);
        mockMvc.perform(multipart("/api/admin/backup/restore").file(file)
                .header(HttpHeaders.AUTHORIZATION, bearer(true)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(content().json("{\"message\":\"Respaldo restaurado correctamente\",\"summary\":{\"tables\":25,\"rows\":3}}"));

        assertEquals("Snapshot Company", jdbc.queryForObject("SELECT nombre FROM empresas WHERE id = 77", String.class));
        assertEquals("Snapshot Project", jdbc.queryForObject("SELECT nombre FROM proyectos WHERE id = 91", String.class));
        Date projectStart = jdbc.queryForObject("SELECT fecha_inicio FROM proyectos WHERE id = 91", Date.class);
        assertNotNull(projectStart);
        assertEquals(LocalDate.of(2026, 3, 4), projectStart.toLocalDate());
        assertEquals(OffsetDateTime.of(2026, 3, 4, 8, 15, 0, 0, ZoneOffset.ofHours(-3)),
            jdbc.queryForObject("SELECT inicio FROM registros_horas WHERE id = 120", (rs, row) -> rs.getObject(1, OffsetDateTime.class)));
        assertNull(jdbc.queryForObject("SELECT descripcion FROM registros_horas WHERE id = 120", String.class));

        Empresa next = empresas.save(new Empresa("After Restore", "AFTER-RESTORE"));
        assertEquals(78L, next.getId());
    }

    @Test
    void rejectsInvalidFileAndMismatchedSchemaBeforeChangingRows() throws Exception {
        insertFixtureRows();
        byte[] snapshot = backupService.exportBackup();
        MockMultipartFile invalid = new MockMultipartFile("file", "broken.json", "application/json", "{bad".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/admin/backup/restore").file(invalid)
                .header(HttpHeaders.AUTHORIZATION, bearer(true)))
            .andExpect(status().isBadRequest());

        com.fasterxml.jackson.databind.node.ObjectNode mismatched = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(snapshot);
        mismatched.put("schemaFingerprint", "not-the-current-schema");
        MockMultipartFile wrongSchema = new MockMultipartFile("file", "wrong-schema.json", "application/json",
            objectMapper.writeValueAsBytes(mismatched));
        mockMvc.perform(multipart("/api/admin/backup/restore").file(wrongSchema)
                .header(HttpHeaders.AUTHORIZATION, bearer(true)))
            .andExpect(status().isBadRequest());
        assertEquals("Snapshot Company", jdbc.queryForObject("SELECT nombre FROM empresas WHERE id = 77", String.class));
    }

    @Test
    void rollsBackDeletesAndEarlierInsertsWhenAnInsertFails() throws Exception {
        insertFixtureRows();
        byte[] snapshot = backupService.exportBackup();
        jdbc.update("DELETE FROM registros_horas WHERE id = ?", 120L);
        jdbc.update("DELETE FROM proyectos WHERE id = ?", 91L);
        jdbc.update("DELETE FROM empresas WHERE id = ?", 77L);
        jdbc.update("INSERT INTO empresas (id, nombre, identificacion_fiscal, limite_usuarios, creado_en) VALUES (?, ?, ?, ?, ?)",
            5L, "Pre-restore state", "PRE-RESTORE", 2, LocalDateTime.of(2025, 1, 1, 0, 0));

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("INSERT INTO \"PROYECTOS\"")) throw new org.springframework.dao.DataIntegrityViolationException("injected insert failure");
            return invocation.callRealMethod();
        }).when(observedJdbc).update(Objects.requireNonNull(anyString()), any(PreparedStatementSetter.class));

        assertThrows(DataAccessException.class, () -> backupService.restore(snapshot));
        assertEquals("Pre-restore state", jdbc.queryForObject("SELECT nombre FROM empresas WHERE id = 5", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM empresas WHERE id = 77", Integer.class));
    }

    @Test
    void rejectsRowsWithWrongWidthBeforeAnyDelete() throws Exception {
        insertFixtureRows();
        com.fasterxml.jackson.databind.node.ObjectNode snapshot = (com.fasterxml.jackson.databind.node.ObjectNode)
            objectMapper.readTree(backupService.exportBackup());
        ArrayNode rows = (ArrayNode) table(snapshot, "EMPRESAS").path("rows");
        ((ArrayNode) rows.get(0)).remove(0);
        assertThrows(BackupValidationException.class, () -> backupService.restore(objectMapper.writeValueAsBytes(snapshot)));
        assertEquals("Snapshot Company", jdbc.queryForObject("SELECT nombre FROM empresas WHERE id = 77", String.class));
    }

    private void insertFixtureRows() {
        jdbc.update("INSERT INTO empresas (id, nombre, identificacion_fiscal, limite_usuarios, creado_en) VALUES (?, ?, ?, ?, ?)",
            77L, "Snapshot Company", "SNAPSHOT-77", 8, LocalDateTime.of(2026, 3, 4, 7, 0));
        jdbc.update("INSERT INTO proyectos (id, nombre, empresa_id, fecha_inicio, descripcion) VALUES (?, ?, ?, ?, ?)",
            91L, "Snapshot Project", 77L, LocalDate.of(2026, 3, 4), null);
        jdbc.update("INSERT INTO registros_horas (id, proyecto_id, inicio, fin, horas, descripcion) VALUES (?, ?, ?, ?, ?, ?)",
            120L, 91L, OffsetDateTime.of(2026, 3, 4, 8, 15, 0, 0, ZoneOffset.ofHours(-3)),
            OffsetDateTime.of(2026, 3, 4, 16, 15, 0, 0, ZoneOffset.ofHours(-3)), 8.0, null);
    }

    private JsonNode table(JsonNode backup, String name) {
        for (JsonNode table : backup.path("tables")) if (name.equalsIgnoreCase(table.path("name").asText())) return table;
        throw new AssertionError("Missing table " + name);
    }

    private String bearer(boolean superAdmin) {
        return "Bearer " + jwtService.generate(1L, 1L, "backup-test@example.invalid", "ADMINISTRADOR", superAdmin);
    }
}
