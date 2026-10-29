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
class TeamInvitationsHttpTest extends TeamInvitationsContract {
    private static final String URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY;
    static { byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes); KEY = Base64.getEncoder().encodeToString(bytes); }
    @DynamicPropertySource static void isolate(DynamicPropertyRegistry r) {
        r.add("app.test-isolation.datasource-url", () -> URL); r.add("spring.datasource.url", () -> URL);
        r.add("app.test-isolation.jwt-secret", () -> KEY); r.add("app.security.jwt-secret", () -> KEY);
        r.add("app.password-mail.enabled", () -> false); r.add("app.superadmin.email", () -> ""); r.add("app.superadmin.password", () -> "");
        for (String suffix : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) r.add("app.stripe." + suffix, () -> "");
    }
    @Override protected void requireOwned() throws Exception {
        try (var c = source.getConnection()) { assertEquals(URL.substring(0, URL.indexOf(';')), c.getMetaData().getURL()); }
    }
}
