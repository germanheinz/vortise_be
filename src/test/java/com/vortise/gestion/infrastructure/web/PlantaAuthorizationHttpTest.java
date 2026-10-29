package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import tools.jackson.databind.ObjectMapper;
import com.vortise.gestion.application.dto.PlantaDto;
import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.repository.PlantaRepository;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.UsuarioDemoSeeder;
import com.vortise.gestion.infrastructure.security.TenantAccessInterceptor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T07–T10 ownership, tenant, authentication and payload tests.
 * Context-starting execution requires separate human approval.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlantaAuthorizationHttpTest {

    private static final String DATABASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String JDBC_URL = DATABASE_URL
        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String JWT_SECRET = generateTestSigningKey();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // Observe the real repository; never stub persistence or ownership decisions.
    @MockitoSpyBean
    private PlantaRepository plantaRepository;

    private Fixtures fixtures;

    // Replace startup runners before the context executes them; no seed writes.
    @MockitoBean
    private DataSeeder dataSeeder;

    @MockitoBean
    private UsuarioDemoSeeder usuarioDemoSeeder;

    @MockitoBean
    private com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder planSuscripcionSeeder;

    @DynamicPropertySource
    static void isolatedProperties(DynamicPropertyRegistry registry) {
        registry.add("app.test-isolation.datasource-url", () -> JDBC_URL);
        registry.add("app.test-isolation.jwt-secret", () -> JWT_SECRET);
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-expiration-ms", () -> 3_600_000L);
    }

    @BeforeEach
    void createFixtures() {
        fixtures = transaction().execute(status -> {
            Empresa a = persistFixture(new Empresa("Auth Test A", "AUTH-TEST-A"));
            Empresa b = persistFixture(new Empresa("Auth Test B", "AUTH-TEST-B"));
            Proyecto a1 = persistFixture(project("A1", a));
            Proyecto a2 = persistFixture(project("A2", a));
            Proyecto b1 = persistFixture(project("B1", b));
            Proyecto companyless = persistFixture(project("No company", null));
            Planta target = persistFixture(plant(a1, "Target"));
            persistFixture(plant(a1, "Sibling"));
            Planta sameCompany = persistFixture(plant(a2, "Same company"));
            Planta foreignCompany = persistFixture(plant(b1, "Foreign company"));
            long missingId = entityManager.createQuery("select max(p.id) from Planta p", Long.class)
                .getSingleResult() + 1;
            assertNull(entityManager.find(Planta.class, missingId));
            long missingProjectId = entityManager.createQuery("select max(p.id) from Proyecto p", Long.class)
                .getSingleResult() + 1;
            assertNull(entityManager.find(Proyecto.class, missingProjectId));
            return new Fixtures(a.getId(), a1.getId(), target.getId(), sameCompany.getId(),
                foreignCompany.getId(), missingId, b1.getId(), companyless.getId(), missingProjectId);
        });
        clearInvocations(plantaRepository);
    }

    @AfterEach
    void removeFixtures() {
        transaction().executeWithoutResult(status -> {
            // Bulk cleanup is restricted to the verified ephemeral test database.
            requireIsolatedDatabaseUnchecked();
            entityManager.createNativeQuery("delete from usuario_empresa_accesos").executeUpdate();
            for (String entity : List.of("Suscripcion", "UsuarioEmpresa", "Planta", "Proyecto", "Empresa")) {
                requireIsolatedDatabaseUnchecked();
                entityManager.createQuery("delete from " + entity).executeUpdate();
            }
        });
    }

    private <T> T persistFixture(T entity) {
        requireIsolatedDatabaseUnchecked();
        entityManager.persist(entity);
        requireIsolatedDatabaseUnchecked();
        entityManager.flush();
        return entity;
    }

    private void requireIsolatedDatabaseUnchecked() {
        try {
            requireIsolatedDatabase();
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot verify the isolated test database", ex);
        }
    }

    void requireIsolatedDatabase() throws SQLException {
        // Supplementary check: startup safety comes from the overrides above.
        // Every fixture persist/flush and cleanup operation calls this explicitly.
        try (Connection connection = dataSource.getConnection()) {
            String actualUrl = connection.getMetaData().getURL();
            if (actualUrl == null || !DATABASE_URL.equals(actualUrl.split(";", 2)[0])) {
                throw new IllegalStateException("Expected the isolated Planta authorization database");
            }
        }
    }

    enum Operation { PUT, DELETE }
    enum Denial { SAME_COMPANY, OTHER_COMPANY, MISSING }

    static Stream<Arguments> ownedOperations() {
        return Stream.of(RolEmpresa.values()).flatMap(role -> Stream.of(Operation.values())
            .map(operation -> Arguments.of(role, operation)));
    }

    static Stream<Arguments> deniedOperations() {
        return Stream.of(RolEmpresa.values()).flatMap(role -> Stream.of(Operation.values())
            .flatMap(operation -> Stream.of(Denial.values())
                .map(denial -> Arguments.of(role, operation, denial))));
    }

    @ParameterizedTest
    @MethodSource("ownedOperations")
    void ownedMutationChangesOnlyTarget(RolEmpresa role, Operation operation) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        ResultActions result = request(role, operation, fixtures.targetId());
        List<PlantaDto> expectedPlants = new ArrayList<>(before.plantas());
        if (operation == Operation.PUT) {
            PlantaDto expected = new PlantaDto(fixtures.targetId(), fixtures.projectId(),
                "Updated", 200.0, 120.0, 80.0, 160.0, 0.8);
            result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expected), true));
            expectedPlants.replaceAll(p -> p.id().equals(fixtures.targetId()) ? expected : p);
            verify(plantaRepository).save(any(Planta.class));
            verify(plantaRepository, never()).deleteById(any());
        } else {
            result.andExpect(status().isNoContent()).andExpect(content().string(""));
            expectedPlants.removeIf(p -> p.id().equals(fixtures.targetId()));
            verify(plantaRepository).deleteById(fixtures.targetId());
            verify(plantaRepository, never()).save(any());
        }
        assertEquals(new DatabaseState(expectedPlants, before.projects(), before.companies()), snapshot());
    }

    @ParameterizedTest
    @MethodSource("deniedOperations")
    void missingOrWrongParentNeverMutates(RolEmpresa role, Operation operation, Denial denial)
            throws Exception {
        Long id = switch (denial) {
            case SAME_COMPANY -> fixtures.sameCompanyId();
            case OTHER_COMPANY -> fixtures.foreignCompanyId();
            case MISSING -> fixtures.missingId();
        };
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        assertNotFound(request(role, operation, id));
        verify(plantaRepository).findById(id);
        assertNoWrites();
        assertEquals(before, snapshot());
    }

    @ParameterizedTest
    @EnumSource(RolEmpresa.class)
    void repeatedDeleteReturnsNotFoundWithoutFurtherChanges(RolEmpresa role) throws Exception {
        request(role, Operation.DELETE, fixtures.targetId())
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        DatabaseState afterDelete = snapshot();
        clearInvocations(plantaRepository);
        assertNotFound(request(role, Operation.DELETE, fixtures.targetId()));
        assertNoWrites();
        assertEquals(afterDelete, snapshot());
    }

    private ResultActions request(RolEmpresa role, Operation operation, Long id) throws Exception {
        String token = identity(role, false);
        return request(token, operation, fixtures.projectId(), id, validBody()).andExpect(result -> {
            assertNotNull(result.getInterceptors());
            assertTrue(java.util.Arrays.stream(result.getInterceptors())
                .anyMatch(TenantAccessInterceptor.class::isInstance),
                "The real tenant interceptor must participate in the request");
        });
    }

    private ResultActions request(String token, Operation operation, Long projectId, Long id,
                                  PlantaDto body) throws Exception {
        String path = "/api/proyectos/" + projectId + "/plantas/" + id;
        var builder = operation == Operation.PUT ? put(path) : delete(path);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (operation == Operation.PUT) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    private static PlantaDto validBody() {
        return new PlantaDto(null, null, "Updated", 200.0, 120.0, 80.0, 160.0, 0.8);
    }

    private String identity(RolEmpresa role, boolean admin) {
        return transaction().execute(status -> HttpIdentityFixtures.token(entityManager, jwtService, fixtures.companyId(), role, admin));
    }

    private String token(RolEmpresa role) {
        return identity(role, false);
    }

    enum ProjectDenial { MISSING, COMPANYLESS, INACCESSIBLE }
    enum TokenDenial { MISSING, MALFORMED, EXPIRED, WRONG_KEY }

    static Stream<Arguments> projectDenials() {
        // Spec 003: ADMINISTRADOR is denied companies they did not create.
        return Stream.of(RolEmpresa.values()).flatMap(role -> Stream.of(Operation.values())
            .flatMap(operation -> Stream.of(ProjectDenial.values())
                .map(denial -> Arguments.of(role, operation, denial))));
    }

    @ParameterizedTest
    @MethodSource("projectDenials")
    void tenantDenialPrecedesChildLookup(RolEmpresa role, Operation operation, ProjectDenial denial)
            throws Exception {
        Long projectId = switch (denial) {
            case MISSING -> fixtures.missingProjectId();
            case COMPANYLESS -> fixtures.companylessProjectId();
            case INACCESSIBLE -> fixtures.foreignProjectId();
        };
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        request(token(role), operation, projectId, fixtures.foreignCompanyId(), validBody())
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertDeniedState(before, true);
    }

    static Stream<Arguments> tokenDenials() {
        return Stream.of(Operation.values()).flatMap(operation -> Stream.of(TokenDenial.values())
            .map(denial -> Arguments.of(operation, denial)));
    }

    @ParameterizedTest
    @MethodSource("tokenDenials")
    void authenticationDenialPrecedesChildLookup(Operation operation, TokenDenial denial) throws Exception {
        String bearer = switch (denial) {
            case MISSING -> null;
            case MALFORMED -> "not-a-jwt";
            case EXPIRED -> signedToken(JWT_SECRET, fixtures.companyId(), Instant.now().minusSeconds(3600));
            case WRONG_KEY -> signedToken(generateTestSigningKey(), fixtures.companyId(), Instant.now().plusSeconds(3600));
        };
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        // Authentication failures are now 401; valid but unauthorized users remain 403.
        request(bearer, operation, fixtures.projectId(), fixtures.targetId(), validBody())
            .andExpect(status().isUnauthorized())
            .andExpect(result -> assertNull(result.getHandler(), "Authentication must fail before MVC"));
        assertDeniedState(before, true);
    }

    private String signedToken(String signingKey, Long companyId, Instant expiration) {
        return Jwts.builder().subject("test@example.invalid").claim("userId", 1L)
            .claim("empresaId", companyId).claim("rol", RolEmpresa.CONSULTA.name())
            .expiration(Date.from(expiration))
            .signWith(Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void legacyTokenWithoutVersionOrCurrentIdentityIsUnauthorized(Operation operation) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        request(signedToken(JWT_SECRET, null, Instant.now().plusSeconds(3600)), operation,
            fixtures.projectId(), fixtures.targetId(), validBody())
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        assertDeniedState(before, true);
    }

    enum ForeignGrant { CREATOR_ADMIN, SUPER_ADMIN }

    static Stream<Arguments> foreignGrants() {
        return Stream.of(Operation.values()).flatMap(operation -> Stream.of(ForeignGrant.values())
            .map(grant -> Arguments.of(operation, grant)));
    }

    @ParameterizedTest
    @MethodSource("foreignGrants")
    void creatorAdminOrSuperAdminCanMutateCorrectlyAddressedForeignCompany(Operation operation, ForeignGrant grant)
            throws Exception {
        String bearer;
        if (grant == ForeignGrant.CREATOR_ADMIN) {
            transaction().executeWithoutResult(status -> {
                requireIsolatedDatabaseUnchecked();
                entityManager.createQuery("update Empresa e set e.creadoPorUsuarioId = :creator where e.identificacionFiscal = 'AUTH-TEST-B'")
                    .setParameter("creator", HttpIdentityFixtures.user(entityManager, fixtures.companyId(), RolEmpresa.ADMINISTRADOR, false).getId()).executeUpdate();
            });
            bearer = token(RolEmpresa.ADMINISTRADOR);
        } else {
            bearer = identity(RolEmpresa.ADMINISTRADOR, true);
        }
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        ResultActions result = request(bearer, operation,
            fixtures.foreignProjectId(), fixtures.foreignCompanyId(), validBody());
        List<PlantaDto> expected = new ArrayList<>(before.plantas());
        if (operation == Operation.PUT) {
            PlantaDto updated = new PlantaDto(fixtures.foreignCompanyId(), fixtures.foreignProjectId(),
                "Updated", 200.0, 120.0, 80.0, 160.0, 0.8);
            result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(updated), true));
            expected.replaceAll(p -> p.id().equals(updated.id()) ? updated : p);
            verify(plantaRepository).save(any());
            verify(plantaRepository, never()).deleteById(any());
        } else {
            result.andExpect(status().isNoContent()).andExpect(content().string(""));
            expected.removeIf(p -> p.id().equals(fixtures.foreignCompanyId()));
            verify(plantaRepository).deleteById(fixtures.foreignCompanyId());
            verify(plantaRepository, never()).save(any());
        }
        assertEquals(new DatabaseState(expected, before.projects(), before.companies()), snapshot());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidNivelReturnsBadRequestWithoutMutation(String nivel) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        request(token(RolEmpresa.CONSULTA), Operation.PUT, fixtures.projectId(), fixtures.targetId(),
            new PlantaDto(null, null, nivel, 200.0, 120.0, 80.0, 160.0, 0.8))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertDeniedState(before, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bodyIdentityCannotOverrideUrlOwnership(boolean wrongParent) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        Long id = wrongParent ? fixtures.foreignCompanyId() : fixtures.targetId();
        PlantaDto body = new PlantaDto(fixtures.sameCompanyId(), fixtures.foreignProjectId(),
            "Updated", 200.0, 120.0, 80.0, 160.0, 0.8);
        ResultActions result = request(token(RolEmpresa.CONSULTA), Operation.PUT,
            fixtures.projectId(), id, body);
        if (wrongParent) {
            assertNotFound(result);
            assertDeniedState(before, false);
        } else {
            PlantaDto expected = new PlantaDto(id, fixtures.projectId(), "Updated", 200.0, 120.0, 80.0, 160.0, 0.8);
            result.andExpect(status().isOk()).andExpect(content().json(objectMapper.writeValueAsString(expected), true));
            List<PlantaDto> plants = before.plantas().stream().map(p -> p.id().equals(id) ? expected : p).toList();
            assertEquals(new DatabaseState(plants, before.projects(), before.companies()), snapshot());
            verify(plantaRepository).save(any());
            verify(plantaRepository, never()).deleteById(any());
        }
    }

    @ParameterizedTest
    @EnumSource(RolEmpresa.class)
    void nullableNumericFieldsRemainSupported(RolEmpresa role) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        PlantaDto expected = new PlantaDto(fixtures.targetId(), fixtures.projectId(), "Updated", null, null, null, null, null);
        request(token(role), Operation.PUT, fixtures.projectId(), fixtures.targetId(),
            new PlantaDto(null, null, "Updated", null, null, null, null, null))
            .andExpect(status().isOk()).andExpect(content().json(objectMapper.writeValueAsString(expected), true));
        List<PlantaDto> plants = before.plantas().stream()
            .map(p -> p.id().equals(fixtures.targetId()) ? expected : p).toList();
        assertEquals(new DatabaseState(plants, before.projects(), before.companies()), snapshot());
        verify(plantaRepository).save(any());
        verify(plantaRepository, never()).deleteById(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void validationPrecedesWrongParentLookup(boolean foreignCompany) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        request(token(RolEmpresa.CONSULTA), Operation.PUT, fixtures.projectId(),
            foreignCompany ? fixtures.foreignCompanyId() : fixtures.sameCompanyId(),
            new PlantaDto(null, null, " ", null, null, null, null, null))
            .andExpect(status().isBadRequest());
        assertDeniedState(before, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void malformedInputReturnsSanitizedClientError(boolean invalidPathId) throws Exception {
        DatabaseState before = snapshot();
        clearInvocations(plantaRepository);
        String childId = invalidPathId ? "not-a-number" : fixtures.targetId().toString();
        // CODE-QUALITY-14 intentionally classifies malformed input as 400 before child lookup.
        mockMvc.perform(put("/api/proyectos/" + fixtures.projectId() + "/plantas/" + childId)
            .header("Authorization", "Bearer " + token(RolEmpresa.CONSULTA))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidPathId ? objectMapper.writeValueAsString(validBody()) : "{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El formato de la solicitud no es válido."));
        assertDeniedState(before, true);
    }

    private void assertDeniedState(DatabaseState before, boolean beforeChildLookup) {
        assertNoWrites();
        if (beforeChildLookup) verify(plantaRepository, never()).findById(any());
        assertEquals(before, snapshot());
    }

    private void assertNotFound(ResultActions result) throws Exception {
        // Exact generic JSON excludes owner/company details for every denial variant.
        result.andExpect(status().isNotFound()).andExpect(content().json(
            "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Planta no encontrada\"}", true));
    }

    private void assertNoWrites() {
        verify(plantaRepository, never()).save(any());
        verify(plantaRepository, never()).deleteById(any());
    }

    private TransactionTemplate transaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private DatabaseState snapshot() {
        // No transaction spans an HTTP request; these are fresh committed-state reads.
        return Objects.requireNonNull(transaction().execute(status -> {
            entityManager.clear();
            List<PlantaDto> plants = entityManager.createQuery("select p from Planta p order by p.id", Planta.class)
                .getResultList().stream().map(p -> new PlantaDto(p.getId(), p.getProyecto().getId(), p.getNivel(),
                    p.getSuperficie(), p.getSupCubierta(), p.getSupDescubierta(), p.getSupCalculada(),
                    p.getCoefPlantaTipo())).toList();
            List<ProjectState> projects = entityManager.createQuery(
                "select p from Proyecto p order by p.id", Proyecto.class).getResultList().stream()
                .map(p -> new ProjectState(p.getId(), p.getNombre(),
                    p.getEmpresaRelacionada() == null ? null : p.getEmpresaRelacionada().getId())).toList();
            List<CompanyState> companies = entityManager.createQuery(
                "select e from Empresa e order by e.id", Empresa.class).getResultList().stream()
                .map(e -> new CompanyState(e.getId(), e.getNombre(), e.getIdentificacionFiscal(),
                    e.getLimiteUsuarios(), e.getCreadoEn())).toList();
            return new DatabaseState(plants, projects, companies);
        }));
    }

    private static Proyecto project(String name, Empresa company) {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(name);
        proyecto.setEmpresaRelacionada(company);
        return proyecto;
    }

    private static Planta plant(Proyecto project, String level) {
        return new Planta(null, project, level, 100.0, 60.0, 40.0, 80.0, 0.5);
    }

    private record Fixtures(Long companyId, Long projectId, Long targetId, Long sameCompanyId,
                            Long foreignCompanyId, Long missingId, Long foreignProjectId,
                            Long companylessProjectId, Long missingProjectId) { }
    private record ProjectState(Long id, String name, Long companyId) { }
    private record CompanyState(Long id, String name, String taxId, int userLimit,
                                java.time.LocalDateTime createdAt) { }
    private record DatabaseState(List<PlantaDto> plantas, List<ProjectState> projects,
                                 List<CompanyState> companies) { }

    private static String generateTestSigningKey() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
