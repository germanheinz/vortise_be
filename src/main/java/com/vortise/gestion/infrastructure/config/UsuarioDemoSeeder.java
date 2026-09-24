package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class UsuarioDemoSeeder implements CommandLineRunner {
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioDemoSeeder(EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository; this.usuarioRepository = usuarioRepository; this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Empresa empresa = empresaRepository.findByNombreIgnoreCase("Constructora Norte")
            .orElseGet(() -> empresaRepository.save(new Empresa("Constructora Norte", "DEMO-0001")));
        if (usuarioRepository.findByEmail("admin@constructora-norte.demo").isPresent()) return;
        usuarioRepository.save(new UsuarioEmpresa(empresa, "admin@constructora-norte.demo", "Administrador demo", passwordEncoder.encode("demo1234"), RolEmpresa.ADMINISTRADOR));
    }
}
