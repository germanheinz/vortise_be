package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.*;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.usecase.*;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import com.vortise.gestion.infrastructure.config.*;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/** Opt-in only. Uses neither old PG lab settings nor any workspace env/config file. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("production")
@EnabledIfSystemProperty(named = "quality14.pg.enabled", matches = "true")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class Quality14PostgresTest {
    private static final String URL = "jdbc:postgresql://127.0.0.1:15438/quality14_test";
    private static final String USER = "quality14_test";
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder demoSeeder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @MockitoBean SuperAdminSeeder adminSeeder;
    @MockitoBean CurrentTenantService identity;
    @MockitoBean com.vortise.gestion.application.port.FotoPerfilStorage photos;
    @Autowired DataSource source;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @MockitoSpyBean ProyectoRepository projects;
    @Autowired RegistroHorasRepository records;
    @Autowired RegistroHorasService ledger;
    @Autowired InicializarEmpresaUseCase initialize;
    @Autowired EmpresaConfiguracionUseCase configuration;
    @Autowired SaveEmpresaUseCase companyEditor;
    @Autowired UsuarioEmpresaUseCase users;
    @Autowired UsuarioEmpresaRepository userRepository;
    @Autowired UnidadMetricaEmpresaRepository units;
    @Autowired CategoriaManoObraEmpresaRepository categories;
    @Autowired CalendarioLaboralEmpresaRepository calendars;

    @DynamicPropertySource static void isolate(DynamicPropertyRegistry r) {
        String password = password();
        byte[] bytes = new byte[64]; new java.security.SecureRandom().nextBytes(bytes);
        String key = Base64.getEncoder().encodeToString(bytes);
        r.add("APP_DATABASE_JDBC_URL", () -> URL); r.add("APP_DATABASE_USERNAME", () -> USER);
        r.add("APP_DATABASE_PASSWORD", () -> password);
        r.add("spring.datasource.url", () -> URL); r.add("spring.datasource.username", () -> USER);
        r.add("spring.datasource.password", () -> password);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); r.add("spring.flyway.enabled", () -> true);
        r.add("spring.sql.init.mode", () -> "never"); r.add("spring.h2.console.enabled", () -> false);
        r.add("app.security.jwt-secret", () -> key); r.add("app.password-mail.enabled", () -> false);
        r.add("app.superadmin.email", () -> ""); r.add("app.superadmin.password", () -> "");
        r.add("app.demo.reset-enabled", () -> false); r.add("app.features.gastos-fijos-enabled", () -> false);
        r.add("APP_ALLOWED_ORIGINS", () -> "https://quality14.example.invalid");
        r.add("APP_FRONTEND_URL", () -> "https://quality14.example.invalid");
        for (String suffix : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) r.add("app.stripe." + suffix, () -> "");
        r.add("logging.level.org.hibernate.SQL", () -> "OFF"); r.add("logging.level.org.hibernate.orm.jdbc.bind", () -> "OFF");
    }
    private static String password() {
        String value = System.getenv("QUALITY14_PG_PASSWORD");
        if (!"true".equals(System.getProperty("quality14.pg.enabled")) || value == null || value.length() < 40) {
            throw new IllegalStateException("Exclusive disposable quality database required");
        }
        return value;
    }
    private void owned() throws Exception {
        try (var connection = source.getConnection()) {
            assertEquals(URL, connection.getMetaData().getURL());
            assertEquals(USER, connection.getMetaData().getUserName());
            assertEquals("public", connection.getSchema());
        }
    }
    private Long company(int limit) throws Exception {
        owned();
        return new TransactionTemplate(tm).execute(status -> {
            Empresa company = new Empresa("Quality " + UUID.randomUUID(), null);
            company.setLimiteUsuarios(limit); em.persist(company); em.flush(); return company.getId();
        });
    }
    private Long project() throws Exception {
        Long companyId = company(5);
        return new TransactionTemplate(tm).execute(status -> {
            Proyecto project = new Proyecto(); project.setNombre("Quality project"); project.setHorasReales(0.0);
            project.setEmpresaRelacionada(em.find(Empresa.class, companyId)); em.persist(project);
            RubroObra rubro = new RubroObra(); rubro.setProyecto(project); rubro.setRubro("Trabajo");
            rubro.setProductividad(null); em.persist(rubro); project.getRubros().add(rubro);
            em.flush(); return project.getId();
        });
    }
    private RegistroHorasCreateDto input(double hours) {
        OffsetDateTime start = OffsetDateTime.parse("2026-10-29T08:00:00Z");
        return new RegistroHorasCreateDto(null, "Trabajo", null, null, null, start, start.plusHours(1),
            null, null, null, null, hours, 0.0, null, null);
    }
    @Test void concurrentHourWritersKeepTheDerivedTotal() throws Exception {
        Long id = project();
        race(() -> ledger.create(id, input(2)), () -> ledger.create(id, input(3)));
        assertEquals(2, records.findByProyectoId(id).size());
        assertEquals(5.0, records.sumHorasByProyectoId(id));
        assertEquals(5.0, projects.findById(id).orElseThrow().getHorasReales());
    }
    @Test void failedTotalUpdateRollsBackTheHourInsert() throws Exception {
        Long id = project();
        doThrow(new IllegalStateException("Synthetic rollback test")).when(projects)
            .save(argThat(p -> p != null && id.equals(p.getId())));
        assertThrows(IllegalStateException.class, () -> ledger.create(id, input(2)));
        assertEquals(0, records.findByProyectoId(id).size());
        assertEquals(0.0, projects.findById(id).orElseThrow().getHorasReales());
    }
    @Test void concurrentInitializationIsUniqueAndIdempotent() throws Exception {
        Long id = company(5);
        race(() -> { initialize.execute(id); return true; }, () -> { initialize.execute(id); return true; });
        assertEquals(5, units.findByEmpresaId(id).size());
        assertEquals(4, categories.findByEmpresaId(id).size());
        assertTrue(calendars.findByEmpresaId(id).isPresent());
    }
    @Test void concurrentCreationReservesTheLastSeatOnlyOnce() throws Exception {
        Long id = company(1);
        List<Boolean> result = race(() -> createUser(id), () -> createUser(id));
        assertEquals(1L, result.stream().filter(Boolean::booleanValue).count());
        assertEquals(1L, userRepository.countByEmpresaIdAndActivoTrue(id));
    }
    @Test void concurrentReactivationReservesTheLastSeatOnlyOnce() throws Exception {
        Long companyId = company(1);
        List<Long> ids = new TransactionTemplate(tm).execute(status -> {
            List<Long> result = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                UsuarioEmpresa user = new UsuarioEmpresa(em.find(Empresa.class, companyId), UUID.randomUUID()+"@example.invalid",
                    "Inactive", "not-a-login-password", RolEmpresa.CONSULTA);
                user.setActivo(false); em.persist(user); em.flush(); result.add(user.getId());
            }
            return result;
        });
        List<Boolean> result = race(() -> activateUser(companyId, ids.get(0)), () -> activateUser(companyId, ids.get(1)));
        assertEquals(1L, result.stream().filter(Boolean::booleanValue).count());
        assertEquals(1L, userRepository.countByEmpresaIdAndActivoTrue(companyId));
    }
    private boolean activateUser(Long companyId, Long userId) {
        try { users.update(companyId, userId, new UsuarioEmpresaUpdateDto("Active", RolEmpresa.CONSULTA, true, null)); return true; }
        catch (ConflictoEstadoException full) { return false; }
    }
    @Test void concurrentDeleteAndEditKeepTheRemainingTotal() throws Exception {
        Long id = project();
        Long first = ledger.create(id, input(2)).id();
        Long second = ledger.create(id, input(3)).id();
        OffsetDateTime start = OffsetDateTime.parse("2026-10-29T08:00:00Z");
        var change = new RegistroHorasUpdateDto(null, "Trabajo", null, null, null, start, start.plusHours(1),
            null, null, null, null, 4.0, 0.0, null, null);
        race(() -> { ledger.delete(first); return true; }, () -> { ledger.update(second, change); return true; });
        assertEquals(1, records.findByProyectoId(id).size());
        assertEquals(4.0, projects.findById(id).orElseThrow().getHorasReales());
        assertEquals(4.0, records.sumHorasByProyectoId(id));
    }
    private boolean createUser(Long companyId) {
        try {
            users.create(companyId, new UsuarioEmpresaDto(null, UUID.randomUUID()+"@example.invalid", "Synthetic",
                RolEmpresa.CONSULTA, true, "Quality14-synthetic-only!", null));
            return true;
        } catch (ConflictoEstadoException full) { return false; }
    }
    @Test void initializationPreservesCreatorAssignedInTheSameTransaction() throws Exception {
        Long id = company(5);
        Long creatorId = new TransactionTemplate(tm).execute(status -> {
            Empresa company = em.find(Empresa.class, id);
            UsuarioEmpresa creator = new UsuarioEmpresa(company, UUID.randomUUID()+"@example.invalid", "Creator", "not-a-login-password", RolEmpresa.ADMINISTRADOR);
            em.persist(creator); em.flush();
            company.setCreadoPorUsuarioId(creator.getId());
            initialize.execute(id);
            assertEquals(creator.getId(), company.getCreadoPorUsuarioId());
            return creator.getId();
        });
        new TransactionTemplate(tm).executeWithoutResult(status -> assertEquals(creatorId, em.find(Empresa.class, id).getCreadoPorUsuarioId()));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void initializationAndRenameRefreshAnEntityReadBeforeAnotherTransactionPreparedIt(boolean rename) throws Exception {
        Long id = company(5);
        CountDownLatch cached = new CountDownLatch(1), prepared = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> staleReader = pool.submit(() -> new TransactionTemplate(tm).executeWithoutResult(status -> {
                assertFalse(em.find(Empresa.class, id).isConfiguracionInicializada());
                cached.countDown();
                try { assertTrue(prepared.await(20, TimeUnit.SECONDS)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                if (rename) companyEditor.update(id, new EmpresaCreateDto("Renamed " + UUID.randomUUID(), null));
                else initialize.execute(id);
            }));
            Future<?> preparer = pool.submit(() -> {
                try { assertTrue(cached.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                initialize.execute(id);
                Long unit = units.findByEmpresaId(id).stream().filter(u -> "ML".equals(u.getCodigo())).findFirst().orElseThrow().getId();
                configuration.deleteUnidad(id, unit);
                prepared.countDown();
            });
            preparer.get(30, TimeUnit.SECONDS); staleReader.get(30, TimeUnit.SECONDS);
            initialize.execute(id);
            assertEquals(4, units.findByEmpresaId(id).size());
        } finally { prepared.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }
    @Test void migrationRefusesDuplicatesWithoutDeletingThem() throws Exception {
        owned();
        String schema = "quality14_duplicates";
        Flyway.configure().dataSource(URL, USER, password()).schemas(schema).defaultSchema(schema).target("8").load().migrate();
        try (var connection = java.sql.DriverManager.getConnection(URL, USER, password())) {
            connection.setSchema(schema);
            try (var statement = connection.createStatement()) {
                Long companyId;
                try (var result = statement.executeQuery("insert into empresas(nombre, limite_usuarios, creado_en) values ('Duplicate fixture',5,now()) returning id")) {
                    assertTrue(result.next()); companyId = result.getLong(1);
                }
                statement.executeUpdate("insert into unidades_metricas_empresa(empresa_id,codigo,nombre) values ("+companyId+",'M2','One'),("+companyId+",'M2','Two')");
            }
        }
        assertThrows(org.flywaydb.core.api.FlywayException.class,
            () -> Flyway.configure().dataSource(URL, USER, password()).schemas(schema).defaultSchema(schema).load().migrate());
        try (var connection = java.sql.DriverManager.getConnection(URL, USER, password())) {
            connection.setSchema(schema);
            try (var statement = connection.createStatement(); var result = statement.executeQuery("select count(*) from unidades_metricas_empresa")) {
                assertTrue(result.next()); assertEquals(2, result.getInt(1));
            }
        }
    }
    private static <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> action : List.of(first, second)) futures.add(pool.submit(() -> {
                ready.countDown(); if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start barrier timeout");
                return action.call();
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            return List.of(futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS));
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }
}
