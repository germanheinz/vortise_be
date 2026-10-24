package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.application.port.FotoPerfilStorage;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AutenticacionUseCaseTest {
    @Mock private UsuarioEmpresaRepository usuarios;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SuscripcionConsultaService suscripciones;
    @Mock private FotoPerfilStorage fotos;
    private AutenticacionUseCase autenticacion;
    private UsuarioEmpresa usuario;

    @BeforeEach
    void setUp() {
        autenticacion = new AutenticacionUseCase(usuarios, passwordEncoder, jwtService, suscripciones, fotos);
        Empresa empresa = new Empresa("Empresa", null);
        usuario = new UsuarioEmpresa(empresa, "user@example.invalid", "Usuario", "hash", RolEmpresa.CONSULTA);
        when(usuarios.findByEmail("user@example.invalid")).thenReturn(Optional.of(usuario));
    }

    @Test
    void successfulActiveLoginStoresTimestampBeforeReturningSession() {
        when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);
        when(suscripciones.planEfectivo(usuario)).thenReturn(Optional.empty());
        when(fotos.existe(null)).thenReturn(false);

        autenticacion.login(new LoginRequestDto("user@example.invalid", "correcta"));

        assertNotNull(usuario.getUltimoLoginExitosoEn());
        verify(usuarios).save(usuario);
    }

    @Test
    void invalidPasswordDoesNotUpdateOrPersistLoginTimestamp() {
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> autenticacion.login(new LoginRequestDto("user@example.invalid", "incorrecta")));

        verify(usuarios, never()).save(any());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getUltimoLoginExitosoEn());
    }

    @Test
    void inactiveUserDoesNotUpdateOrPersistLoginTimestamp() {
        usuario.setActivo(false);

        assertThrows(IllegalArgumentException.class,
            () -> autenticacion.login(new LoginRequestDto("user@example.invalid", "correcta")));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(usuarios, never()).save(any());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getUltimoLoginExitosoEn());
    }
}
