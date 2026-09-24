package com.vortise.gestion.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioDemoSeederTest {

    @Test
    void shouldCreateDemoCompanyAndAdminUserWhenCompanyDoesNotExist() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository usuarioRepository = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        Empresa empresa = new Empresa("Constructora Norte", "DEMO-0001");
        when(empresaRepository.findByNombreIgnoreCase("Constructora Norte")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresa);
        when(usuarioRepository.findByEmail("admin@constructora-norte.demo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("demo1234")).thenReturn("encoded-demo-password");

        new UsuarioDemoSeeder(empresaRepository, usuarioRepository, passwordEncoder).run();

        verify(empresaRepository).save(any(Empresa.class));
        verify(usuarioRepository).save(org.mockito.ArgumentMatchers.argThat(usuario ->
            "admin@constructora-norte.demo".equals(usuario.getEmail())
                && "Constructora Norte".equals(usuario.getEmpresa().getNombre())
                && "encoded-demo-password".equals(usuario.getPasswordHash())
                && RolEmpresa.ADMINISTRADOR == usuario.getRol()
        ));
    }
}
