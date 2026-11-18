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
    @Mock private LoginThrottle throttle;
    private static final String IP = "203.0.113.1";
    private AutenticacionUseCase autenticacion;
    private UsuarioEmpresa usuario;

    @BeforeEach
    void setUp() {
        autenticacion = new AutenticacionUseCase(usuarios, passwordEncoder, jwtService, suscripciones, fotos, org.mockito.Mockito.mock(EquipoAccessPolicy.class), throttle);
        Empresa empresa = new Empresa("Empresa", null);
        usuario = new UsuarioEmpresa(empresa, "user@example.invalid", "Usuario", "hash", RolEmpresa.CONSULTA);
        org.mockito.Mockito.lenient().when(usuarios.findByEmail("user@example.invalid")).thenReturn(Optional.of(usuario));
    }

    @Test
    void successfulActiveLoginStoresTimestampBeforeReturningSession() {
        when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);
        when(suscripciones.planEfectivo(usuario)).thenReturn(Optional.empty());
        when(fotos.existe(null)).thenReturn(false);

        autenticacion.login(new LoginRequestDto("user@example.invalid", "correcta"), IP);

        assertNotNull(usuario.getUltimoLoginExitosoEn());
        verify(usuarios).save(usuario);
        verify(throttle).limpiarCuenta("user@example.invalid");
        verify(throttle, never()).registrarFallo(any(), any());
    }

    @Test
    void invalidPasswordDoesNotUpdateOrPersistLoginTimestamp() {
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> autenticacion.login(new LoginRequestDto("user@example.invalid", "incorrecta"), IP));

        verify(usuarios, never()).save(any());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getUltimoLoginExitosoEn());
        verify(throttle).registrarFallo("user@example.invalid", IP);
        verify(throttle, never()).limpiarCuenta(any());
    }

    @Test
    void inactiveUserDoesNotUpdateOrPersistLoginTimestamp() {
        usuario.setActivo(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> autenticacion.login(new LoginRequestDto("user@example.invalid", "correcta"), IP));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(usuarios, never()).save(any());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getUltimoLoginExitosoEn());
        verify(throttle).registrarFallo("user@example.invalid", IP);
    }

    @Test
    void unknownEmailCountsAsFailedAttemptWithTheSameError() {
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> autenticacion.login(new LoginRequestDto("nadie@example.invalid", "x"), IP));

        verify(throttle).registrarFallo("nadie@example.invalid", IP);
    }

    @Test
    void emailIsNormalizedBeforeThrottlingAndLookup() {
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> autenticacion.login(new LoginRequestDto("  USER@Example.Invalid ", "incorrecta"), IP));

        verify(throttle).bloqueado("user@example.invalid", IP);
        verify(throttle).registrarFallo("user@example.invalid", IP);
    }

    @Test
    void blockedClientIsRejectedBeforeAnyPasswordCheck() {
        when(throttle.bloqueado("user@example.invalid", IP)).thenReturn(true);

        assertThrows(com.vortise.gestion.application.exception.DemasiadosIntentosException.class,
            () -> autenticacion.login(new LoginRequestDto("user@example.invalid", "correcta"), IP));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(usuarios, never()).findByEmail(any());
        verify(throttle, never()).registrarFallo(any(), any());
    }

    @Test
    void storageFailureWhileCountingStillReportsInvalidCredentials() {
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("down"))
            .when(throttle).registrarFallo(any(), any());

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> autenticacion.login(new LoginRequestDto("user@example.invalid", "incorrecta"), IP));
    }
}
