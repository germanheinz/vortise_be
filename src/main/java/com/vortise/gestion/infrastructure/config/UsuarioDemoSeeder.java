package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class UsuarioDemoSeeder implements CommandLineRunner {
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedRunPolicy seedRunPolicy;

    public UsuarioDemoSeeder(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this(empresaRepository, usuarioRepository, passwordEncoder, () -> true);
    }

    @Autowired
    public UsuarioDemoSeeder(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository,
            PasswordEncoder passwordEncoder, SeedRunPolicy seedRunPolicy) {
        this.empresaRepository = empresaRepository; this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder; this.seedRunPolicy = seedRunPolicy;
    }

    @Override
    public void run(String... args) {
        if (!seedRunPolicy.shouldRunSeeders()) return;
        Empresa empresa = empresaRepository.findByIdentificacionFiscal("DEMO-0001")
            .orElseGet(() -> {
                if (empresaRepository.findByNombreIgnoreCase("Constructora Norte").isPresent()) {
                    throw new IllegalStateException("El nombre de la empresa demo pertenece a otra identificación fiscal");
                }
                return empresaRepository.save(new Empresa("Constructora Norte", "DEMO-0001"));
            });
        if (usuarioRepository.findByEmail("admin@constructora-norte.demo").isPresent()) return;
        usuarioRepository.save(new UsuarioEmpresa(empresa, "admin@constructora-norte.demo", "Administrador demo", passwordEncoder.encode("demo1234"), RolEmpresa.ADMINISTRADOR));
    }
}
