package com.vortise.gestion.infrastructure.web;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vortise.gestion.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.RubroObra;
import com.vortise.gestion.service.authentication.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Spec 021: contract, tenant isolation and recalculation of {@code GET /api/proyectos/{id}/indicadores}. UUID memory H2 only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class IndicadoresObraHttpTest {
    private static final String BASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String URL = BASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    private static final String SUBRUBRO = "$.rubros[0].pisos[0].subrubros[0]";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwt;
    @Autowired DataSource source;
    @PersistenceContext EntityManager em;
    @MockitoBean com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder planSuscripcionSeeder;
    private Proyecto own, other;
    private RubroObra platea;
    private String token;

    private static String signingKey() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
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
            assertEquals(BASE_URL, connection.getMetaData().getURL().split(";", 2)[0]);
        }
        var a = new Empresa("Indicadores A", "IND-A");
        em.persist(a);
        var b = new Empresa("Indicadores B", "IND-B");
        em.persist(b);
        own = project(a, "A");
        other = project(b, "B");
        platea = new RubroObra();
        platea.setProyecto(own); platea.setRubro("Hormigón"); platea.setSubRubro("Platea"); platea.setUnidad("m3");
        platea.setCantidad(20.); platea.setHorasOficialesPrevistas(50.); platea.setHorasAyudantesPrevistas(50.);
        platea.setCantidadPersonas(2);
        em.persist(platea);
        own.getRubros().add(platea);
        em.persist(new CategoriaManoObraEmpresa(a, "OFICIAL", 5000., true));
        em.persist(new CategoriaManoObraEmpresa(a, "AYUDANTE", 4000., true));
        a.inicializarCalendarioLaboral();
        em.flush();
        token = HttpIdentityFixtures.token(em, jwt, a.getId(), RolEmpresa.ADMINISTRADOR, false);
    }

    private Proyecto project(Empresa company, String name) {
        var p = new Proyecto();
        p.setNombre(name); p.setDireccion("Test"); p.setNumeroProyecto(name); p.setEmpresaRelacionada(company);
        em.persist(p);
        return p;
    }

    private String cargas() { return "/api/proyectos/" + own.getId() + "/obra-tareas"; }

    private String carga(double oficiales, double ayudantes) throws Exception {
        return mapper.writeValueAsString(Map.of("nombre", "Platea", "rubroId", platea.getId(), "fecha", "2026-10-14",
            "dias", 3, "cantidadReal", 10, "horasOficiales", oficiales, "horasAyudantes", ayudantes));
    }

    private long crearCarga() throws Exception {
        var creada = mvc.perform(post(cargas()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(carga(40, 40)))
            .andExpect(status().isCreated()).andReturn();
        em.flush(); em.clear();
        return mapper.readTree(creada.getResponse().getContentAsString()).path("id").asLong();
    }

    @Test void returnsEveryLevelOfTheDocumentDataset() throws Exception {
        crearCarga();
        mvc.perform(get("/api/proyectos/" + own.getId() + "/indicadores").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parametros.horasJornada").value(8.0))
            .andExpect(jsonPath(SUBRUBRO + ".presupuesto.costo").value(closeTo(450_000, 1e-6)))
            .andExpect(jsonPath(SUBRUBRO + ".presupuesto.diasPrevistosRedondeados").value(7))
            .andExpect(jsonPath(SUBRUBRO + ".hhPrevistasFecha").value(closeTo(48, 1e-9)))
            .andExpect(jsonPath(SUBRUBRO + ".indices.estadoProductividad").value("BAJA"))
            .andExpect(jsonPath(SUBRUBRO + ".jornales").value(4))
            .andExpect(jsonPath(SUBRUBRO + ".tareas[0].personasPorDia").value(1))
            .andExpect(jsonPath(SUBRUBRO + ".desvio").value(closeTo(-135_000, 1e-6)))
            .andExpect(jsonPath("$.obra.desvioPorcentaje").value(closeTo(-0.6, 1e-9)))
            .andExpect(jsonPath("$.obra.proyeccionTotal").value(closeTo(-135_000, 1e-6)));
    }

    @Test void editingOrDeletingALoadRecalculates() throws Exception {
        long id = crearCarga();
        mvc.perform(put(cargas() + "/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(carga(24, 24)))
            .andExpect(status().isOk());
        em.flush(); em.clear();
        mvc.perform(get("/api/proyectos/" + own.getId() + "/indicadores").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath(SUBRUBRO + ".desvio").value(closeTo(225_000 - 216_000, 1e-6)))
            .andExpect(jsonPath(SUBRUBRO + ".tareas[0].personasPrevistas").value(true));

        mvc.perform(delete(cargas() + "/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        em.flush(); em.clear();
        mvc.perform(get("/api/proyectos/" + own.getId() + "/indicadores").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath(SUBRUBRO + ".conAvance").value(false))
            .andExpect(jsonPath("$.obra.desvio").doesNotExist());
    }

    @Test void requiresAuthenticationAndCompanyAccess() throws Exception {
        mvc.perform(get("/api/proyectos/" + own.getId() + "/indicadores")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/proyectos/" + other.getId() + "/indicadores").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
