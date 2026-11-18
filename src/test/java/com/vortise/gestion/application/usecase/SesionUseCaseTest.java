package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SesionUseCaseTest {
    private final AutenticacionUseCase auth = mock(AutenticacionUseCase.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final UsuarioEmpresaRepository users = mock(UsuarioEmpresaRepository.class);
    private final SesionUseCase useCase = new SesionUseCase(auth, refresh, users);

    private static LoginResponseDto dto(String token) {
        return new LoginResponseDto(token, 5L, 2L, "Empresa", "Nombre", "e@example.invalid", "JEFE_DE_OBRA", false, true, "PRO", false, List.of(7L));
    }

    @Test
    void openingASessionKeepsTheTokensOutOfTheBodyAndKeepsTheRest() {
        when(refresh.emitir(5L)).thenReturn("refresh-raw");

        var sesion = useCase.abrir(dto("access-jwt"));

        assertEquals("access-jwt", sesion.accessToken());
        assertEquals("refresh-raw", sesion.refreshToken());
        assertNull(sesion.datos().token());
        assertEquals(List.of(7L), sesion.datos().obras());
        assertEquals("JEFE_DE_OBRA", sesion.datos().rol());
    }

    @Test
    void refreshingRotatesTheTokenAndBuildsTheSessionFromTheCurrentAccount() {
        var user = new UsuarioEmpresa(new Empresa("Empresa", null), "e@example.invalid", "Nombre", "h", RolEmpresa.JEFE_DE_OBRA);
        when(refresh.rotar("old")).thenReturn(new RefreshTokenService.Rotacion(5L, "new-refresh"));
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(auth.sesion(user)).thenReturn(dto("new-access"));

        var sesion = useCase.refrescar("old");

        assertEquals("new-access", sesion.accessToken());
        assertEquals("new-refresh", sesion.refreshToken());
        assertNull(sesion.datos().token());
    }

    @Test
    void anInvalidRefreshTokenPropagatesAsAnInvalidSession() {
        when(refresh.rotar("bad")).thenThrow(new SesionInvalidaException());

        assertThrows(SesionInvalidaException.class, () -> useCase.refrescar("bad"));
    }

    @Test
    void logoutDelegatesToTheTokenChain() {
        useCase.cerrar("raw");

        verify(refresh).cerrar("raw");
    }
}
