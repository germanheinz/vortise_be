package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.infrastructure.config.*;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class Quality14HttpTest {
    private static final String URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY;
    static { byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes); KEY = Base64.getEncoder().encodeToString(bytes); }
    @DynamicPropertySource static void isolate(DynamicPropertyRegistry r) {
        r.add("app.test-isolation.datasource-url", () -> URL); r.add("spring.datasource.url", () -> URL);
        r.add("app.test-isolation.jwt-secret", () -> KEY); r.add("app.security.jwt-secret", () -> KEY);
        r.add("app.password-mail.enabled", () -> false); r.add("app.superadmin.email", () -> ""); r.add("app.superadmin.password", () -> "");
        for (String suffix : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) r.add("app.stripe." + suffix, () -> "");
    }
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder demoSeeder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @MockitoBean SuperAdminSeeder adminSeeder;
    @MockitoBean com.vortise.gestion.application.port.FotoPerfilStorage photos;
    @Autowired DataSource source;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    private Long companyId, foreignId, projectId, readerId;
    private String admin, reader;

    @BeforeEach void fixturesOnlyAfterMetadataGuard() throws Exception {
        // H2 metadata reports the exact memory identity without connection options.
        // The pre-DataSource profile guard validates those options before startup.
        try (var connection = source.getConnection()) {
            assertEquals(URL.substring(0, URL.indexOf(';')), connection.getMetaData().getURL());
        }
        new TransactionTemplate(tm).executeWithoutResult(status -> {
            Empresa own = new Empresa("Quality HTTP " + UUID.randomUUID(), null); em.persist(own);
            Empresa other = new Empresa("Other HTTP " + UUID.randomUUID(), null); em.persist(other); em.flush();
            companyId = own.getId(); foreignId = other.getId();
            for (int i = 0; i < 3; i++) {
                Proyecto project = new Proyecto(); project.setNombre("Project " + i); project.setEmpresaRelacionada(own);
                em.persist(project); em.flush(); if (i == 0) projectId = project.getId();
            }
            Proyecto foreign = new Proyecto(); foreign.setNombre("Foreign"); foreign.setEmpresaRelacionada(other); em.persist(foreign);
            admin = "Bearer " + HttpIdentityFixtures.token(em, jwt, companyId, RolEmpresa.ADMINISTRADOR, false);
            readerId = HttpIdentityFixtures.user(em, companyId, RolEmpresa.CONSULTA, false).getId();
            reader = "Bearer " + HttpIdentityFixtures.token(em, jwt, companyId, RolEmpresa.CONSULTA, false);
        });
    }
    @Test void paginatedContractKeepsLegacyResultsAndRejectsForeignCompany() throws Exception {
        mvc.perform(get("/api/proyectos/pagina").param("size", "2").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/proyectos/pagina").param("size", "2").param("page", "1").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/proyectos").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/empresas/"+foreignId+"/obras/pagina").header("Authorization", admin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/proyectos/pagina").param("size", "101").header("Authorization", admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/proyectos/pagina")).andExpect(status().isUnauthorized());
    }
    @Test void preparationIsExplicitIdempotentAndAdministratorOnly() throws Exception {
        String path = "/api/empresas/"+companyId+"/configuracion";
        mvc.perform(get(path+"/unidades").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(path+"/calendario").header("Authorization", admin)).andExpect(status().isNotFound());
        mvc.perform(post(path+"/inicializar").header("Authorization", reader)).andExpect(status().isForbidden());
        mvc.perform(post(path+"/inicializar").header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(post(path+"/inicializar").header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get(path+"/unidades").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get(path+"/categorias-mano-obra").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(post("/api/empresas/"+foreignId+"/configuracion/inicializar").header("Authorization", admin)).andExpect(status().isForbidden());
    }
    @Test void editingUserDoesNotRequireEmailAndRejectsShortReplacementPassword() throws Exception {
        String path = "/api/empresas/"+companyId+"/usuarios/"+readerId;
        mvc.perform(put(path).header("Authorization", admin).contentType("application/json")
            .content("{\"nombre\":\"Updated\",\"rol\":\"CONSULTA\",\"activo\":true}"))
            .andExpect(status().isOk());
        mvc.perform(put(path).header("Authorization", admin).contentType("application/json")
            .content("{\"nombre\":\"Updated\",\"rol\":\"CONSULTA\",\"activo\":true,\"password\":\"a\"}"))
            .andExpect(status().isBadRequest());
        assertTrue("not-a-login-password".equals(em.find(UsuarioEmpresa.class, readerId).getPasswordHash()));
    }
    @Test void malformedJsonAndParametersAreSanitizedClientErrors() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"password\":\"PRIVATE-FORMAT-MARKER\","))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El formato de la solicitud no es válido."))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE-FORMAT-MARKER"))));
        mvc.perform(get("/api/proyectos/pagina").header("Authorization", admin).param("page", "invalid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("El formato de la solicitud no es válido."));
        mvc.perform(get("/api/proyectos/not-a-number").header("Authorization", admin))
            .andExpect(status().isBadRequest());
    }
    @Test void invalidHourRangeIsInputErrorWithoutPersistingAnything() throws Exception {
        mvc.perform(post("/api/proyectos/"+projectId+"/registro-horas").header("Authorization", admin)
            .contentType("application/json").content("{\"rubro\":\"Trabajo\",\"inicio\":\"2026-10-29T09:00:00Z\",\"fin\":\"2026-10-29T08:00:00Z\"}"))
            .andExpect(status().isBadRequest());
        assertEquals(0L, em.createQuery("select count(h) from RegistroHoras h where h.proyecto.id = :id", Long.class)
            .setParameter("id", projectId).getSingleResult());
    }
}
