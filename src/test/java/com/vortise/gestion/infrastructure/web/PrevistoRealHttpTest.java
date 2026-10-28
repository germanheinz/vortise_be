package com.vortise.gestion.infrastructure.web;

import tools.jackson.databind.ObjectMapper;
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
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Execution needs reviewed approval: real HTTP/security/JPA with UUID memory H2 only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class PrevistoRealHttpTest {
    private static final String BASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String URL = BASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwt;
    @Autowired DataSource source;
    @PersistenceContext EntityManager em;
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder usuarioDemoSeeder;
    @MockitoBean com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder planSuscripcionSeeder;
    private Proyecto own, other;
    private RubroObra activity, foreignActivity;
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
        var a = new Empresa("Comparison A","CMP-A"); em.persist(a);
        var b = new Empresa("Comparison B","CMP-B"); em.persist(b);
        own = project(a,"A"); other = project(b,"B");
        activity = activity(own); foreignActivity = activity(other);
        em.persist(new CategoriaManoObraEmpresa(a,"OFICIAL",2000.,true));
        em.persist(new CategoriaManoObraEmpresa(a,"AYUDANTE",1000.,true));
        em.persist(new CalendarioLaboralEmpresa(a));
        em.flush();
        token = HttpIdentityFixtures.token(em, jwt, a.getId(), RolEmpresa.JEFE_DE_OBRA, false);
    }
    private Proyecto project(Empresa company,String name) {
        var p = new Proyecto(); p.setNombre(name); p.setDireccion("Test"); p.setNumeroProyecto(name);
        p.setEmpresaRelacionada(company); em.persist(p); return p;
    }
    private RubroObra activity(Proyecto p) {
        var r = new RubroObra(); r.setProyecto(p); r.setRubro("Hormigón"); r.setSubRubro("Fundación");
        r.setCantidad(155.4); r.setUnidad("m3"); r.setHorasOficialesPrevistas(4000.); r.setHorasAyudantesPrevistas(1881.5);
        em.persist(r); p.getRubros().add(r); return r;
    }
    private String path(Proyecto p) { return "/api/proyectos/"+p.getId()+"/obra-tareas"; }
    private String body(Long id) throws Exception {
        return mapper.writeValueAsString(Map.of("nombre","Carga","rubroId",id,"fecha","2026-10-14","cantidadReal",6,"horasOficiales",100,"horasAyudantes",80));
    }
    @Test void savesOwnedLoadAndReadsExcelMetrics() throws Exception {
        mvc.perform(post(path(own)).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body(activity.getId())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rubroId").value(activity.getId()));
        em.flush(); em.clear();
        mvc.perform(get(path(own)+"/indicadores").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.actividades[0].cantidadReal").value(6.0))
            .andExpect(jsonPath("$.actividades[0].indicadores.desvio").value(org.hamcrest.Matchers.closeTo(101525.096525,0.001)));
    }
    @Test void foreignActivityCannotBeLinkedToAccessibleProject() throws Exception {
        mvc.perform(post(path(own)).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body(foreignActivity.getId())))
            .andExpect(status().isNotFound());
        assertEquals(0L,em.createQuery("select count(t) from ObraTarea t",Long.class).getSingleResult());
    }
    @Test void comparisonRequiresAuthenticationAndCompanyAccess() throws Exception {
        mvc.perform(get(path(own)+"/indicadores")).andExpect(status().isUnauthorized());
        mvc.perform(get(path(other)+"/indicadores").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
    }
    @Test void savingPlanKeepsTaskLinkAndDerivesHours() throws Exception {
        var t = new ObraTarea(); t.setProyecto(own); t.setActividad(activity); t.setNombre("Daily"); em.persist(t); em.flush();
        var payload = Map.of("nombre","A","direccion","Test","numeroProyecto","A","rubros", List.of(Map.of(
            "id",activity.getId(),"rubro","Hormigón","cantidad",155.4,"unidad","m3","rendimientoOficial",10,"rendimientoAyudante",5)));
        mvc.perform(put("/api/proyectos/"+own.getId()).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rubros[0].id").value(activity.getId()))
            .andExpect(jsonPath("$.rubros[0].horasOficialesPrevistas").value(1554.));
        em.flush(); em.clear();
        assertEquals(activity.getId(),em.find(ObraTarea.class,t.getId()).getRubroId());
    }
    @Test void deletingProjectAlsoRemovesLinkedTasks() throws Exception {
        var t = new ObraTarea(); t.setProyecto(own); t.setActividad(activity); t.setNombre("Daily");
        em.persist(t); own.getObraTareas().add(t); em.flush();
        mvc.perform(delete("/api/proyectos/"+own.getId()).header("Authorization","Bearer "+token))
            .andExpect(status().isNoContent());
        em.flush(); em.clear();
        assertNull(em.find(ObraTarea.class,t.getId()));
        assertNull(em.find(RubroObra.class,activity.getId()));
    }

    @Test void cannotMovePlanToForeignCompany() throws Exception {
        var payload = Map.of("nombre","A","direccion","Test","numeroProyecto","A","empresaId",other.getEmpresaRelacionada().getId());
        mvc.perform(put("/api/proyectos/"+own.getId()).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
        assertNotEquals(other.getEmpresaRelacionada().getId(),own.getEmpresaRelacionada().getId());
    }

}
