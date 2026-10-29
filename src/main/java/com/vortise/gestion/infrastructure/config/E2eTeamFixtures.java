package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.application.usecase.InicializarEmpresaUseCase;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Synthetic data exclusively behind the pre-DataSource disposable E2E guard. */
@Component
@Profile("e2e-local")
public class E2eTeamFixtures implements CommandLineRunner {
    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final PlanSuscripcionRepository planes;
    private final SuscripcionRepository suscripciones;
    private final PasswordEncoder encoder;
    private final InicializarEmpresaUseCase inicializar;
    private final String password;
    public E2eTeamFixtures(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios, PlanSuscripcionRepository planes,
            SuscripcionRepository suscripciones, PasswordEncoder encoder, InicializarEmpresaUseCase inicializar,
            @Value("${APP_E2E_TEAM_PASSWORD}") String password) {
        this.empresas = empresas; this.usuarios = usuarios; this.planes = planes; this.suscripciones = suscripciones;
        this.encoder = encoder; this.inicializar = inicializar; this.password = password;
    }
    @Override @Transactional public void run(String... args) {
        if (usuarios.findByEmail("team-admin@example.invalid").isPresent()) throw new IllegalStateException("Disposable E2E fixture must be new");
        Empresa first = empresas.save(new Empresa("E2E team company A", null));
        UsuarioEmpresa owner = usuarios.save(new UsuarioEmpresa(first, "team-admin@example.invalid", "Admin E2E", encoder.encode(password), RolEmpresa.ADMINISTRADOR));
        first.setCreadoPorUsuarioId(owner.getId()); empresas.save(first); inicializar.execute(first.getId());
        for (String name : List.of("E2E team company B", "E2E team company excluded")) {
            Empresa company = new Empresa(name, null); company.setCreadoPorUsuarioId(owner.getId()); company = empresas.save(company); inicializar.execute(company.getId());
        }
        PlanSuscripcion pro = planes.findByCodigo("PRO").orElseGet(() -> planes.save(new PlanSuscripcion("PRO", "Pro", 9900, "usd", 0, 2, List.of("Equipo de cinco invitados"))));
        Suscripcion subscription = Suscripcion.paraUsuario(owner, "cus_e2e_team_"+UUID.randomUUID());
        subscription.actualizarDesdeStripe("sub_e2e_team_"+UUID.randomUUID(), pro, "active", null, Instant.now().plusSeconds(86400), false);
        suscripciones.save(subscription);
    }
}
