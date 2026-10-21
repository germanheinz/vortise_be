package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class SuperAdminSeeder implements CommandLineRunner {
    static final String PLATFORM_TAX_ID = "PLATFORM-0001";
    static final String PLATFORM_NAME = "Plataforma";
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public SuperAdminSeeder(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.superadmin.email:}") String email,
            @Value("${app.superadmin.password:}") String password) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(String... args) {
        if (email.isEmpty()) return;
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Super admin not created: APP_SUPERADMIN_PASSWORD must have at least {} characters", MIN_PASSWORD_LENGTH);
            return;
        }
        var existing = usuarioRepository.findByEmail(email);
        if (existing.isPresent()) {
            if (!existing.get().isSuperAdmin()) {
                log.warn("Super admin not created: the configured email already belongs to a regular user");
            }
            return;
        }
        Empresa plataforma = empresaRepository.findByIdentificacionFiscal(PLATFORM_TAX_ID)
            .orElseGet(() -> {
                if (empresaRepository.findByNombreIgnoreCase(PLATFORM_NAME).isPresent()) {
                    throw new IllegalStateException("El nombre de la empresa de plataforma pertenece a otra identificación fiscal");
                }
                return empresaRepository.save(new Empresa(PLATFORM_NAME, PLATFORM_TAX_ID));
            });
        UsuarioEmpresa superAdmin = new UsuarioEmpresa(plataforma, email, "Super administrador",
            passwordEncoder.encode(password), RolEmpresa.ADMINISTRADOR);
        superAdmin.setSuperAdmin(true);
        usuarioRepository.save(superAdmin);
    }
}
