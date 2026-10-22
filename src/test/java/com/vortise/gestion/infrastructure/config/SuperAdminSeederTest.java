package com.vortise.gestion.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SuperAdminSeederTest {
    private static final String EMAIL = "root@example.invalid";
    private static final String PASSWORD = "long-enough-password";

    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void withoutConfiguredEmailDoesNothing() {
        seeder("", PASSWORD).run();
        verifyNoInteractions(empresas, usuarios, encoder);
    }

    @Test
    void shortPasswordDoesNothing() {
        seeder(EMAIL, "short").run();
        verifyNoInteractions(empresas, usuarios, encoder);
    }

    @Test
    void createsPlatformCompanyAndFlaggedAdministrator() {
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(empresas.findByIdentificacionFiscal(SuperAdminSeeder.PLATFORM_TAX_ID)).thenReturn(Optional.empty());
        when(empresas.findByNombreIgnoreCase(SuperAdminSeeder.PLATFORM_NAME)).thenReturn(Optional.empty());
        when(empresas.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode(PASSWORD)).thenReturn("hash");

        seeder(" Root@Example.invalid ", PASSWORD).run();

        verify(usuarios).save(argThat(user -> user.isSuperAdmin()
            && EMAIL.equals(user.getEmail())
            && RolEmpresa.ADMINISTRADOR == user.getRol()
            && "hash".equals(user.getPasswordHash())
            && SuperAdminSeeder.PLATFORM_TAX_ID.equals(user.getEmpresa().getIdentificacionFiscal())));
    }

    @Test
    void createsConfiguredSuperAdminWhenDatabaseIsAlreadyInitialized() {
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(empresas.findByIdentificacionFiscal(SuperAdminSeeder.PLATFORM_TAX_ID)).thenReturn(Optional.empty());
        when(empresas.findByNombreIgnoreCase(SuperAdminSeeder.PLATFORM_NAME)).thenReturn(Optional.empty());
        when(empresas.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode(PASSWORD)).thenReturn("hash");

        new SuperAdminSeeder(empresas, usuarios, encoder, EMAIL, PASSWORD, () -> false).run();

        verify(usuarios).save(argThat(user -> user.isSuperAdmin() && EMAIL.equals(user.getEmail())
            && "hash".equals(user.getPasswordHash())));
    }

    @Test
    void existingSuperAdminIsLeftUntouched() {
        UsuarioEmpresa existing = mock(UsuarioEmpresa.class);
        when(existing.isSuperAdmin()).thenReturn(true);
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        seeder(EMAIL, PASSWORD).run();
        verify(usuarios, never()).save(any());
        verifyNoInteractions(empresas, encoder);
    }

    @Test
    void existingRegularUserIsNeverPromoted() {
        UsuarioEmpresa existing = mock(UsuarioEmpresa.class);
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        seeder(EMAIL, PASSWORD).run();
        verify(existing, never()).setSuperAdmin(any(Boolean.class));
        verify(usuarios, never()).save(any());
        verifyNoInteractions(empresas, encoder);
    }

    @Test
    void conflictingPlatformNameIsNotReused() {
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(empresas.findByIdentificacionFiscal(SuperAdminSeeder.PLATFORM_TAX_ID)).thenReturn(Optional.empty());
        when(empresas.findByNombreIgnoreCase(SuperAdminSeeder.PLATFORM_NAME))
            .thenReturn(Optional.of(new Empresa(SuperAdminSeeder.PLATFORM_NAME, "OTHER")));
        assertThrows(IllegalStateException.class, () -> seeder(EMAIL, PASSWORD).run());
        verify(empresas, never()).save(any());
        verify(usuarios, never()).save(any());
    }

    private SuperAdminSeeder seeder(String email, String password) {
        return new SuperAdminSeeder(empresas, usuarios, encoder, email, password);
    }
}
