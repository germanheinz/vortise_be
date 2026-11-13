package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.UsuarioDemoSeeder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Guardado real (con commit y claves foráneas) de la estructura de actividades. Execution needs reviewed approval:
 * real HTTP/security/JPA with UUID memory H2 only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RubroEstructuraPersistenciaTest {
    private static final String BASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String URL = BASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwt;
    @Autowired DataSource source;
    @Autowired PlatformTransactionManager transactions;
    @PersistenceContext EntityManager em;
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder usuarioDemoSeeder;
    @MockitoBean com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder planSuscripcionSeeder;
    private Long proyectoId;
    private Long subcontratistaId;
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
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var empresa = new Empresa("Estructura " + UUID.randomUUID(), "EST-" + UUID.randomUUID().toString().substring(0, 8));
            em.persist(empresa);
            var proyecto = new Proyecto();
            proyecto.setNombre("A");
            proyecto.setDireccion("Test");
            proyecto.setNumeroProyecto("A");
            proyecto.setEmpresaRelacionada(empresa);
            em.persist(proyecto);
            var subcontratista = new com.vortise.gestion.domain.model.CatalogoEmpresa(empresa, "SUBCONTRATISTA", "SUB1", "Subcontratista Test");
            em.persist(subcontratista);
            proyecto.getSubcontratistas().add(subcontratista);
            subcontratistaId = subcontratista.getId();
            fila(proyecto, "5000", "Hormigón", null);
            fila(proyecto, "5001", "Hormigón", "Fundaciones");
            fila(proyecto, "5002", "Hormigón", "Losas");
            em.flush();
            proyectoId = proyecto.getId();
            token = HttpIdentityFixtures.token(em, jwt, empresa.getId(), RolEmpresa.JEFE_DE_OBRA, false);
        });
    }

    private void fila(Proyecto proyecto, String codigo, String rubro, String subRubro) {
        var fila = new RubroObra();
        fila.setProyecto(proyecto);
        fila.setnRubro(codigo);
        fila.setRubro(rubro);
        fila.setSubRubro(subRubro);
        if (subRubro != null) { fila.setCantidad(10.); fila.setUnidad("m3"); }
        em.persist(fila);
        proyecto.getRubros().add(fila);
    }

    private List<Long> idsGuardados() {
        return new TransactionTemplate(transactions).execute(status ->
            em.createQuery("select r.id from RubroObra r where r.proyecto.id = :id order by r.id", Long.class)
                .setParameter("id", proyectoId).getResultList());
    }

    private org.springframework.test.web.servlet.ResultActions guardar(List<Map<String, Object>> rubros) throws Exception {
        var payload = Map.of("nombre", "A", "direccion", "Test", "numeroProyecto", "A", "rubros", rubros);
        return mvc.perform(put("/api/proyectos/" + proyectoId).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)));
    }

    private Map<String, Object> legado(Long id, String codigo, String rubro, String subRubro) {
        var fila = new java.util.LinkedHashMap<String, Object>();
        fila.put("id", id);
        fila.put("nRubro", codigo);
        fila.put("rubro", rubro);
        if (subRubro != null) { fila.put("subRubro", subRubro); fila.put("cantidad", 10); fila.put("unidad", "m3"); }
        return fila;
    }

    @Test void savesNewThreeLayerRubroNextToLegacyRows() throws Exception {
        var ids = idsGuardados();
        guardar(List.of(
            legado(ids.get(0), "5000", "Hormigón", null), legado(ids.get(1), "5001", "Hormigón", "Fundaciones"),
            legado(ids.get(2), "5002", "Hormigón", "Losas"),
            Map.of("nRubro", "1", "tipoNodo", "RUBRO", "varianteEstructura", "RUBRO_NIVEL_SUBRUBRO", "nombre", "Nuevo", "rubro", "Nuevo"),
            Map.of("nRubro", "1.1", "tipoNodo", "NIVEL", "parentCodigo", "1", "nombre", "Sobre fundaciones", "rubro", "Nuevo"),
            Map.of("nRubro", "1.1.1", "tipoNodo", "SUBRUBRO", "parentCodigo", "1.1", "nombre", "Pilotes", "rubro", "Nuevo", "subRubro", "Pilotes",
                "cantidad", 4, "unidad", "m3")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rubros.length()").value(6));
    }

    @Test void savesNewRootAloneAfterRemovingEveryLegacyRow() throws Exception {
        var ids = idsGuardados();
        // Primero un guardado legado que enlaza 5001/5002 con 5000 (parent_id), como el que hace la grilla.
        guardar(List.of(legado(ids.get(0), "5000", "Hormigón", null), legado(ids.get(1), "5001", "Hormigón", "Fundaciones"),
            legado(ids.get(2), "5002", "Hormigón", "Losas"))).andExpect(status().isOk());
        guardar(List.of(Map.of("nRubro", "1", "tipoNodo", "RUBRO", "varianteEstructura", "RUBRO_NIVEL_SUBRUBRO", "nombre", "Nuevo", "rubro", "Nuevo")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rubros.length()").value(1));
    }

    private org.springframework.test.web.servlet.ResultActions guardar(List<Map<String, Object>> rubros, List<Map<String, Object>> contratos) throws Exception {
        var payload = Map.of("nombre", "A", "direccion", "Test", "numeroProyecto", "A", "rubros", rubros, "contratos", contratos);
        return mvc.perform(put("/api/proyectos/" + proyectoId).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)));
    }

    private Map<String, Object> raiz(Object contratoId) {
        var fila = new java.util.LinkedHashMap<String, Object>(Map.of("nRubro", "1", "tipoNodo", "RUBRO", "varianteEstructura", "RUBRO_SUBRUBRO", "nombre", "Nuevo", "rubro", "Nuevo"));
        if (contratoId != null) fila.put("contratoId", contratoId);
        return fila;
    }

    private static final Map<String, Object> HOJA = Map.of("nRubro", "1.1", "tipoNodo", "SUBRUBRO", "parentCodigo", "1", "nombre", "Pilotes",
        "rubro", "Nuevo", "subRubro", "Pilotes", "cantidad", 4, "unidad", "m3");

    @Test void assignsAProjectContractToTheRootRubroAndProtectsContractsInUse() throws Exception {
        var creado = guardar(List.of(raiz(null), HOJA), List.of(Map.of("nombre", "  Contrato 1 ")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contratos[0].nombre").value("Contrato 1")).andReturn();
        long contratoId = mapper.readTree(creado.getResponse().getContentAsString()).get("contratos").get(0).get("id").asLong();

        guardar(List.of(raiz(contratoId), HOJA), List.of(Map.of("id", contratoId, "nombre", "Contrato 1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rubros[?(@.nRubro=='1')].contratoId").value((int) contratoId))
            .andExpect(jsonPath("$.rubros[?(@.nRubro=='1')].contratoNombre").value("Contrato 1"))
            .andExpect(jsonPath("$.rubros[?(@.nRubro=='1.1')].contratoId").value(org.hamcrest.Matchers.contains((Object) null)));

        guardar(List.of(raiz(contratoId), HOJA), List.of()).andExpect(status().isBadRequest());
        guardar(List.of(raiz(999999), HOJA), List.of(Map.of("id", contratoId, "nombre", "Contrato 1"))).andExpect(status().isBadRequest());
        guardar(List.of(raiz(null), HOJA), List.of(Map.of("id", contratoId, "nombre", "Contrato 1"), Map.of("nombre", "contrato 1")))
            .andExpect(status().isBadRequest());
        guardar(List.of(raiz(null), HOJA), List.of()).andExpect(status().isOk()).andExpect(jsonPath("$.contratos.length()").value(0));
    }

    @Test void keepsTheContractorOfAContractAcrossSaves() throws Exception {
        var contratos = List.<Map<String, Object>>of(Map.of("nombre", "Contrato Sub", "subcontratistaId", subcontratistaId));
        var creado = guardar(List.of(raiz(null), HOJA), contratos).andExpect(status().isOk())
            .andExpect(jsonPath("$.contratos[0].subcontratistaId").value((int) (long) subcontratistaId))
            .andExpect(jsonPath("$.contratos[0].subcontratistaNombre").value("Subcontratista Test")).andReturn();
        long contratoId = mapper.readTree(creado.getResponse().getContentAsString()).get("contratos").get(0).get("id").asLong();
        guardar(List.of(raiz(contratoId), HOJA), List.of(Map.of("id", contratoId, "nombre", "Contrato Sub", "subcontratistaId", subcontratistaId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contratos[0].subcontratistaNombre").value("Subcontratista Test"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/proyectos/" + proyectoId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contratos[0].subcontratistaNombre").value("Subcontratista Test"));
    }
}
