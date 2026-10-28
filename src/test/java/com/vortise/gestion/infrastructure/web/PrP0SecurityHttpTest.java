package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.vortise.gestion.application.dto.GastoFijoDto;
import com.vortise.gestion.application.dto.PresupuestoLineaDto;
import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder;
import com.vortise.gestion.infrastructure.config.ProductionFeaturePolicy;
import com.vortise.gestion.infrastructure.config.SuperAdminSeeder;
import com.vortise.gestion.infrastructure.config.UsuarioDemoSeeder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * PR-P0-SEC-01 HTTP regression coverage. Context startup is isolated and requires
 * separate execution approval; seeders are mocked before their runners execute.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("null")
class PrP0SecurityHttpTest {
    private static final String DATABASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String JDBC_URL = DATABASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String JWT_SECRET = generateTestSigningKey();
    private static final String FEATURE_UNAVAILABLE_CODE = "FEATURE_TEMPORARILY_UNAVAILABLE";

    @Autowired private DataSource dataSource;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProductionFeaturePolicy featurePolicy;
    @PersistenceContext private EntityManager entityManager;

    @MockitoSpyBean private ProyectoRepository proyectoRepository;
    @MockitoSpyBean private TareaRepository tareaRepository;
    @MockitoSpyBean private PresupuestoLineaRepository presupuestoLineaRepository;
    @MockitoSpyBean private GastoFijoRepository gastoFijoRepository;
    @MockitoSpyBean private IngresoRepository ingresoRepository;

    @MockitoBean private DataSeeder dataSeeder;
    @MockitoBean private UsuarioDemoSeeder usuarioDemoSeeder;
    @MockitoBean private PlanSuscripcionSeeder planSuscripcionSeeder;
    @MockitoBean private SuperAdminSeeder superAdminSeeder;

    private Fixtures fixtures;

    @DynamicPropertySource
    static void isolatedProperties(DynamicPropertyRegistry registry) {
        registry.add("app.test-isolation.datasource-url", () -> JDBC_URL);
        registry.add("app.test-isolation.jwt-secret", () -> JWT_SECRET);
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-expiration-ms", () -> 3_600_000L);
        registry.add("app.environment.synthetic-data", () -> false);
        registry.add("app.environment.storage-isolated", () -> false);
        registry.add("app.features.gastos-fijos-enabled", () -> false);
        registry.add("app.demo.reset-enabled", () -> false);
    }

    @BeforeEach
    void createCommittedFixtures() {
        assertFalse(featurePolicy.gastosFijosEnabled());
        assertFalse(featurePolicy.demoResetEnabled());
        fixtures = transaction().execute(status -> {
            Empresa ownCompany = persistFixture(new Empresa("P0 own " + UUID.randomUUID(), "P0-A-" + UUID.randomUUID()));
            Empresa foreignCompany = persistFixture(new Empresa("P0 foreign " + UUID.randomUUID(), "P0-B-" + UUID.randomUUID()));
            Proyecto ownProject = persistFixture(project("Own project", ownCompany));
            Proyecto foreignProject = persistFixture(project("Foreign project", foreignCompany));
            PresupuestoLinea foreignLine = new PresupuestoLinea(null, foreignProject, "Etapa original",
                "Superficie original", "Función original", "Sistema original", 17.0, 2.5, 31.0, 8.0, 4);
            persistFixture(foreignLine);
            GastoFijo fixedCost = persistFixture(new GastoFijo(null, "Fixture global", 1234.5, "Sintético", 7));
            return new Fixtures(ownCompany.getId(), ownProject.getId(), foreignProject.getId(),
                foreignLine.getId(), fixedCost.getId());
        });
        clearInvocations(proyectoRepository, tareaRepository, presupuestoLineaRepository,
            gastoFijoRepository, ingresoRepository);
    }

    @AfterEach
    void removeFixturesFromVerifiedEphemeralDatabase() {
        transaction().executeWithoutResult(status -> {
            for (String entity : List.of("PresupuestoLinea", "GastoFijo", "UsuarioEmpresa", "Planta", "Proyecto", "Empresa")) {
                requireIsolatedDatabaseUnchecked();
                entityManager.createQuery("delete from " + entity).executeUpdate();
            }
        });
    }

    @Test
    void dashboardUsesRealJwtAndDeniesCrossTenantBeforeReadingKpis() throws Exception {
        clearInvocations(proyectoRepository, tareaRepository, presupuestoLineaRepository,
            gastoFijoRepository, ingresoRepository);
        mockMvc.perform(get("/api/dashboard").param("proyectoId", fixtures.ownProjectId().toString())
                .header("Authorization", bearer(fixtures.ownCompanyId(), RolEmpresa.CONSULTA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kpis.costoIndirectoDisponible").value(false))
            .andExpect(jsonPath("$.kpis.costoIndirectoArs").value((Object) null))
            .andExpect(jsonPath("$.kpis.balanceArs").value((Object) null));
        verify(tareaRepository).findByEntregaProyectoId(fixtures.ownProjectId());
        verify(gastoFijoRepository, never()).findAll();

        clearInvocations(proyectoRepository, tareaRepository, presupuestoLineaRepository,
            gastoFijoRepository, ingresoRepository);
        mockMvc.perform(get("/api/dashboard").param("proyectoId", fixtures.foreignProjectId().toString())
                .header("Authorization", bearer(fixtures.ownCompanyId(), RolEmpresa.CONSULTA)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verify(proyectoRepository).findById(fixtures.foreignProjectId());
        verify(tareaRepository, never()).findByEntregaProyectoId(any());
        verify(presupuestoLineaRepository, never()).findByProyectoId(any());
        verify(ingresoRepository, never()).findByProyectoId(any());
        verify(gastoFijoRepository, never()).findAll();
    }

    @Test
    void budgetPutAndDeleteRejectForeignChildWithoutChangingCommittedState() throws Exception {
        BudgetState before = budgetState();
        PresupuestoLineaDto attemptedUpdate = new PresupuestoLineaDto(fixtures.foreignLineId(),
            fixtures.ownProjectId(), "Etapa alterada", "Otra superficie", "Otra función", "Otro sistema",
            999.0, 99.0, 999.0, 999.0, 12);

        mockMvc.perform(put(budgetPath()).header("Authorization", bearer(fixtures.ownCompanyId(), RolEmpresa.CONSULTA))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(attemptedUpdate)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Presupuesto no encontrado"));
        assertEquals(before, budgetState());
        verify(presupuestoLineaRepository).findById(fixtures.foreignLineId());
        verify(presupuestoLineaRepository, never()).save(any());
        verify(presupuestoLineaRepository, never()).deleteById(any());

        clearInvocations(presupuestoLineaRepository);
        mockMvc.perform(delete(budgetPath()).header("Authorization", bearer(fixtures.ownCompanyId(), RolEmpresa.CONSULTA)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Presupuesto no encontrado"));
        assertEquals(before, budgetState());
        verify(presupuestoLineaRepository).findById(fixtures.foreignLineId());
        verify(presupuestoLineaRepository, never()).save(any());
        verify(presupuestoLineaRepository, never()).deleteById(any());
    }

    @Test
    void disabledFixedCostEndpointsReturnStable503WithoutReadingOrMutatingRows() throws Exception {
        List<FixedCostState> before = fixedCostState();
        String auth = bearer(fixtures.ownCompanyId(), RolEmpresa.ADMINISTRADOR);
        GastoFijoDto validBody = new GastoFijoDto(null, "Attempted insert", 99.0, "Synthetic", 8);
        assertFeatureUnavailable(mockMvc.perform(get("/api/gastos-fijos").header("Authorization", auth)));
        assertFeatureUnavailable(mockMvc.perform(post("/api/gastos-fijos").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validBody))));
        assertFeatureUnavailable(mockMvc.perform(put("/api/gastos-fijos/" + fixtures.fixedCostId())
            .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validBody))));
        assertFeatureUnavailable(mockMvc.perform(delete("/api/gastos-fijos/" + fixtures.fixedCostId())
            .header("Authorization", auth)));

        assertEquals(before, fixedCostState());
        verify(gastoFijoRepository, never()).findAll();
        verify(gastoFijoRepository, never()).findById(any());
        verify(gastoFijoRepository, never()).save(any());
        verify(gastoFijoRepository, never()).deleteById(any());
    }

    @Test
    void disabledCashflowReturns503BeforeUseCaseQueriesProjectData() throws Exception {
        clearInvocations(proyectoRepository, presupuestoLineaRepository, gastoFijoRepository, ingresoRepository);
        assertFeatureUnavailable(mockMvc.perform(get("/api/proyectos/" + fixtures.ownProjectId() + "/cashflow")
            .header("Authorization", bearer(fixtures.ownCompanyId(), RolEmpresa.CONSULTA))));

        // TenantAccessInterceptor must load the URL project once; the use case must not query it again.
        verify(proyectoRepository, times(1)).findById(fixtures.ownProjectId());
        verify(presupuestoLineaRepository, never()).findByProyectoId(any());
        verify(gastoFijoRepository, never()).findAll();
        verify(ingresoRepository, never()).findByProyectoId(any());
    }

    @Test
    void demoResetReturns503AndNeverCallsSeeder() throws Exception {
        assertFeatureUnavailable(mockMvc.perform(post("/api/proyectos/reset-demo")
            .header("Authorization", bearer(fixtures.ownCompanyId(), RolEmpresa.ADMINISTRADOR))));
        verify(dataSeeder, never()).resetDemoData();
    }

    private void assertFeatureUnavailable(ResultActions result) throws Exception {
        result.andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.code").value(FEATURE_UNAVAILABLE_CODE));
    }

    private String budgetPath() {
        return "/api/proyectos/" + fixtures.ownProjectId() + "/presupuesto/" + fixtures.foreignLineId();
    }

    private String bearer(Long companyId, RolEmpresa role) {
        return "Bearer " + transaction().execute(status -> HttpIdentityFixtures.token(entityManager, jwtService, companyId, role, false));
    }

    private BudgetState budgetState() {
        return Objects.requireNonNull(transaction().execute(status -> {
            entityManager.clear();
            PresupuestoLinea line = entityManager.find(PresupuestoLinea.class, fixtures.foreignLineId());
            assertNotNull(line);
            return new BudgetState(line.getId(), line.getProyecto().getId(), line.getEtapa(),
                line.getTipoSuperficie(), line.getFuncion(), line.getSistema(), line.getM2(),
                line.getHhPorM2(), line.getPrecioPorHh(), line.getPrecioPorHhUsd(), line.getMes());
        }));
    }

    private List<FixedCostState> fixedCostState() {
        return Objects.requireNonNull(transaction().execute(status -> {
            entityManager.clear();
            return entityManager.createQuery("select g from GastoFijo g order by g.id", GastoFijo.class)
                .getResultList().stream().map(g -> new FixedCostState(g.getId(), g.getConcepto(),
                    g.getMonto(), g.getCategoria(), g.getMes())).toList();
        }));
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
            throw new IllegalStateException("Cannot verify the isolated PR-P0 test database", ex);
        }
    }

    private void requireIsolatedDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String actualUrl = connection.getMetaData().getURL();
            assertTrue(actualUrl != null && DATABASE_URL.equals(actualUrl.split(";", 2)[0]),
                "Fixture writes and cleanup must use this suite's generated in-memory H2 database");
        }
    }

    private TransactionTemplate transaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private static Proyecto project(String name, Empresa company) {
        Proyecto project = new Proyecto();
        project.setNombre(name);
        project.setEmpresaRelacionada(company);
        return project;
    }

    private static String generateTestSigningKey() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private record Fixtures(Long ownCompanyId, Long ownProjectId, Long foreignProjectId,
                            Long foreignLineId, Long fixedCostId) { }
    private record BudgetState(Long id, Long projectId, String stage, String surface, String function,
                               String system, Double squareMeters, Double hoursPerSquareMeter,
                               Double pricePerHour, Double usdPricePerHour, Integer month) { }
    private record FixedCostState(Long id, String concept, Double amount, String category, Integer month) { }
}
