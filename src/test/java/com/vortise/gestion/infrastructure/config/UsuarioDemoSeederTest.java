package com.vortise.gestion.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
    @Test
    void reusesRenamedCompanyAndPreservesExistingUserOnRepeatedStartup() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Empresa renamed = new Empresa("Renamed", "DEMO-0001");
        renamed.setLimiteUsuarios(12);
        var existingUser = mock(com.vortise.gestion.domain.model.UsuarioEmpresa.class);
        when(empresas.findByIdentificacionFiscal("DEMO-0001")).thenReturn(Optional.of(renamed));
        when(usuarios.findByEmail(any())).thenReturn(Optional.of(existingUser));
        var seeder = new UsuarioDemoSeeder(empresas, usuarios, encoder);
        seeder.run();
        seeder.run();
        verify(empresas, never()).save(any());
        verify(empresas, never()).findByNombreIgnoreCase(any());
        verify(usuarios, never()).save(any());
        verifyNoInteractions(encoder, existingUser);
        assertEquals("Renamed", renamed.getNombre());
        assertEquals(12, renamed.getLimiteUsuarios());
    }

    @Test
    void newUserBelongsToRenamedCompany() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Empresa renamed = new Empresa("Renamed", "DEMO-0001");
        when(empresas.findByIdentificacionFiscal("DEMO-0001")).thenReturn(Optional.of(renamed));
        new UsuarioDemoSeeder(empresas, usuarios, encoder).run();
        verify(usuarios).save(org.mockito.ArgumentMatchers.argThat(user -> user.getEmpresa() == renamed));
        verify(empresas, never()).save(any());
    }

    @Test
    void conflictingNameDoesNotReuseForeignCompanyOrWrite() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(empresas.findByNombreIgnoreCase("Constructora Norte"))
            .thenReturn(Optional.of(new Empresa("Constructora Norte", "OTHER")));
        assertThrows(IllegalStateException.class, () -> new UsuarioDemoSeeder(empresas, usuarios, encoder).run());
        verify(empresas, never()).save(any());
        verifyNoInteractions(usuarios, encoder);
    }
}
