package com.vortise.gestion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.UsuarioDemoSeeder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Execution needs reviewed approval: real HTTP/security/JPA with UUID memory H2 only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class RestriccionDetalleHttpTest {
    private static final String BASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String URL = BASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwt;
    @Autowired DataSource source;
    @PersistenceContext EntityManager em;
    @MockBean DataSeeder dataSeeder;
    @MockBean UsuarioDemoSeeder usuarioDemoSeeder;
    private Proyecto own, other;
    private Long restrictionId, foreignRestrictionId;
    private String token;

    private static String signingKey() {
        byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    @DynamicPropertySource static void isolation(DynamicPropertyRegistry p) {
        p.add("app.test-isolation.datasource-url", () -> URL);
        p.add("spring.datasource.url", () -> URL);
        p.add("app.test-isolation.jwt-secret", () -> KEY);
        p.add("app.security.jwt-secret", () -> KEY);
    }
    @BeforeEach void fixtures() throws Exception {
        try (var connection = source.getConnection()) {
            assertEquals(BASE_URL, connection.getMetaData().getURL().split(";",2)[0]);
        }
        var a = new Empresa("Comparison A","CMP-A-"+UUID.randomUUID()); em.persist(a);
        var b = new Empresa("Comparison B","CMP-B-"+UUID.randomUUID()); em.persist(b);
        own = project(a,"A"); other = project(b,"B");
        restrictionId = restriction(own); foreignRestrictionId = restriction(other);
        em.persist(new CategoriaManoObraEmpresa(a,"OFICIAL",2000.,true));
        em.persist(new CategoriaManoObraEmpresa(a,"AYUDANTE",1000.,true));
        em.persist(new CalendarioLaboralEmpresa(a));
        em.flush();
        token = jwt.generate(1L,a.getId(),"comparison@example.invalid","JEFE_DE_OBRA");
        TestTransaction.flagForCommit();
        TestTransaction.end(); // No test transaction may hide lazy loading failures during HTTP requests.
    }
    private Proyecto project(Empresa company,String name) {
        var p = new Proyecto(); p.setNombre(name); p.setDireccion("Test"); p.setNumeroProyecto(name);
        p.setEmpresaRelacionada(company); em.persist(p); return p;
    }
    private Long restriction(Proyecto p) {
        var r = new RestriccionObra(); r.setProyecto(p); r.setRestriccion("Acopio pendiente");
        r.setEstado("PENDIENTE"); r.setResponsable("Responsable demo"); em.persist(r); em.flush();
        return r.getId();
    }
    @Test void detailAndCommentsWorkWithoutOpenSessionInView() throws Exception {
        mvc.perform(get("/api/restricciones/"+restrictionId).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proyectoNombre").value("A"))
            .andExpect(jsonPath("$.restriccion").value("Acopio pendiente"));
        mvc.perform(get("/api/restricciones/"+restrictionId+"/comentarios").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void foreignCompanyAndAnonymousRequestsRemainDenied() throws Exception {
        mvc.perform(get("/api/restricciones/"+foreignRestrictionId).header("Authorization","Bearer "+token))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/restricciones/"+restrictionId)).andExpect(status().isForbidden());
    }
}
