package com.vortise.gestion.service.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.exception.AccesoDenegadoException;
import com.vortise.gestion.application.port.IdentidadExternaVerificador.IdentidadVerificada;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentidadExternaServiceTest {
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final AutenticacionService autenticacion = mock(AutenticacionService.class);
    private final IdentidadExternaService useCase = new IdentidadExternaService(usuarios, autenticacion);

    private static UsuarioEmpresa user(String email) {
        return new UsuarioEmpresa(new Empresa("E", null), email, "Name", "hash", RolEmpresa.CONSULTA);
    }

    @Test
    void aLinkedActiveUserOpensASession() {
        UsuarioEmpresa linked = user("ana@example.invalid");
        linked.vincularIdentidadExterna("user_ana");
        when(usuarios.findByClerkUserId("user_ana")).thenReturn(Optional.of(linked));
        useCase.iniciarSesion(new IdentidadVerificada("user_ana", "ana@example.invalid"));

        verify(autenticacion).sesion(linked);
        verify(usuarios, never()).findByEmail(any());
    }

    @Test
    void anExistingUserIsLinkedByVerifiedEmailOnFirstSignIn() {
        UsuarioEmpresa existing = user("ana@example.invalid");
        when(usuarios.findByEmail("ana@example.invalid")).thenReturn(Optional.of(existing));

        useCase.iniciarSesion(new IdentidadVerificada("user_ana", " Ana@Example.invalid "));

        assertEquals("user_ana", existing.getClerkUserId());
        verify(usuarios).save(existing);
    }

    @Test
    void anUnknownEmailOrAUserLinkedToAnotherAccountGetsNoSession() {
        UsuarioEmpresa linkedElsewhere = user("bob@example.invalid");
        linkedElsewhere.vincularIdentidadExterna("user_bob");
        when(usuarios.findByEmail("bob@example.invalid")).thenReturn(Optional.of(linkedElsewhere));

        var unknown = assertThrows(AccesoDenegadoException.class,
            () -> useCase.iniciarSesion(new IdentidadVerificada("user_new", "stranger@example.invalid")));
        var stolen = assertThrows(AccesoDenegadoException.class,
            () -> useCase.iniciarSesion(new IdentidadVerificada("user_attacker", "bob@example.invalid")));

        assertEquals(AccesoDenegadoException.CUENTA_NO_REGISTRADA, unknown.getCode());
        assertEquals(AccesoDenegadoException.CUENTA_NO_REGISTRADA, stolen.getCode());
        assertEquals("user_bob", linkedElsewhere.getClerkUserId());
        verify(usuarios, never()).save(any());
        verify(autenticacion, never()).sesion(any());
    }

    @Test
    void anInactiveUserCannotSignIn() {
        UsuarioEmpresa inactive = user("ana@example.invalid");
        inactive.vincularIdentidadExterna("user_ana");
        inactive.setActivo(false);
        when(usuarios.findByClerkUserId("user_ana")).thenReturn(Optional.of(inactive));

        var denied = assertThrows(AccesoDenegadoException.class,
            () -> useCase.iniciarSesion(new IdentidadVerificada("user_ana", "ana@example.invalid")));

        assertEquals(AccesoDenegadoException.CUENTA_INACTIVA, denied.getCode());
        verify(autenticacion, never()).sesion(any());
    }
}
