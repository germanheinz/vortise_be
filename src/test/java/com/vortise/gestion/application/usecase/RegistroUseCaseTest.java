package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class RegistroUseCaseTest {
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AutenticacionUseCase autenticacion = mock(AutenticacionUseCase.class);
    private final RegistroUseCase useCase = new RegistroUseCase(empresas, usuarios, encoder, autenticacion);
    private final RegistroRequestDto request = new RegistroRequestDto(" Nueva Constructora ", " Ana ", " Ana@Example.invalid ", "password123");

    @BeforeEach
    void setUp() {
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(empresas.findByNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(empresas.save(any(Empresa.class))).thenAnswer(inv -> {
            Empresa e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 3L);
            return e;
        });
        when(usuarios.save(any(UsuarioEmpresa.class))).thenAnswer(inv -> {
            UsuarioEmpresa u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 11L);
            return u;
        });
        when(encoder.encode("password123")).thenReturn("hash");
    }

    @Test
    void createsIsolatedCompanyWithCreatorAdministratorAndLogsIn() {
        LoginResponseDto session = new LoginResponseDto("token", 11L, 3L, "Nueva Constructora", "Ana", "ana@example.invalid",
            "ADMINISTRADOR", false, false, null, false);
        when(autenticacion.sesion(any())).thenReturn(session);

        assertEquals(session, useCase.registrar(request));

        verify(usuarios).save(org.mockito.ArgumentMatchers.argThat(u -> "ana@example.invalid".equals(u.getEmail())
            && "Ana".equals(u.getNombre()) && "hash".equals(u.getPasswordHash())
            && u.getRol() == RolEmpresa.ADMINISTRADOR && !u.isSuperAdmin()
            && "Nueva Constructora".equals(u.getEmpresa().getNombre())));
        verify(empresas, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.argThat(e -> "Nueva Constructora".equals(e.getNombre())));
        verify(autenticacion).sesion(org.mockito.ArgumentMatchers.argThat(u -> Long.valueOf(11L).equals(u.getEmpresa().getCreadoPorUsuarioId())));
    }

    @Test
    void duplicateEmailOrCompanyIsRejectedWithSameGenericMessage() {
        when(usuarios.findByEmail("ana@example.invalid")).thenReturn(Optional.of(mock(UsuarioEmpresa.class)));
        var emailTaken = assertThrows(SolicitudInvalidaException.class, () -> useCase.registrar(request));

        when(usuarios.findByEmail("ana@example.invalid")).thenReturn(Optional.empty());
        when(empresas.findByNombreIgnoreCase("Nueva Constructora")).thenReturn(Optional.of(new Empresa("Nueva Constructora", null)));
        var companyTaken = assertThrows(SolicitudInvalidaException.class, () -> useCase.registrar(request));

        assertEquals(RegistroUseCase.MENSAJE_DUPLICADO, emailTaken.getMessage());
        assertEquals(emailTaken.getMessage(), companyTaken.getMessage());
        verify(empresas, never()).save(any());
        verify(usuarios, never()).save(any());
    }
}
