package com.vortise.gestion.infrastructure.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

class SeedersSkipOnInitializedDatabaseTest {
    @Test
    void stagingPreservesExplicitSuperAdminBootstrapAndReadsCatalog() {
        SeedRunPolicy skipSeeders = new StagingSeedRunPolicy();
        EmpresaRepository platformEmpresas = mock(EmpresaRepository.class);
        UsuarioEmpresaRepository platformUsuarios = mock(UsuarioEmpresaRepository.class);
        PasswordEncoder platformEncoder = mock(PasswordEncoder.class);
        when(platformUsuarios.findByEmail("root@example.invalid")).thenReturn(Optional.empty());
        when(platformEmpresas.findByIdentificacionFiscal(SuperAdminSeeder.PLATFORM_TAX_ID)).thenReturn(Optional.empty());
        when(platformEmpresas.findByNombreIgnoreCase(SuperAdminSeeder.PLATFORM_NAME)).thenReturn(Optional.empty());
        when(platformEmpresas.save(any(com.vortise.gestion.domain.model.Empresa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(platformEncoder.encode("long-enough-password")).thenReturn("password-hash");
        new SuperAdminSeeder(platformEmpresas, platformUsuarios, platformEncoder,
            "root@example.invalid", "long-enough-password", skipSeeders).run();
        verify(platformUsuarios).save(argThat(user -> user.isSuperAdmin()
            && "root@example.invalid".equals(user.getEmail())
            && "password-hash".equals(user.getPasswordHash())));

        PlanSuscripcionRepository planes = mock(PlanSuscripcionRepository.class);
        new PlanSuscripcionSeeder(planes, "", "", skipSeeders).run();
        verify(planes).findByCodigo("BASIC");
        verify(planes).findByCodigo("PRO");
        verifyNoMoreInteractions(planes);

    }

}