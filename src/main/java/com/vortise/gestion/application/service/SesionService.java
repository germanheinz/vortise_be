package com.vortise.gestion.application.service;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns an authenticated account into a browser session: a short access token and a rotating refresh token,
 * both delivered as HttpOnly cookies. The response body never carries either token.
 */
@Service
public class SesionService {
    /** {@code datos} is safe to return to the browser; the tokens go only into cookies. */
    public record Sesion(LoginResponseDto datos, String accessToken, String refreshToken) { }

    private final AutenticacionService autenticacion;
    private final RefreshTokenService refreshTokens;
    private final UsuarioEmpresaRepository usuarios;

    public SesionService(AutenticacionService autenticacion, RefreshTokenService refreshTokens, UsuarioEmpresaRepository usuarios) {
        this.autenticacion = autenticacion;
        this.refreshTokens = refreshTokens;
        this.usuarios = usuarios;
    }

    public Sesion abrir(LoginResponseDto autenticada) {
        return new Sesion(sinToken(autenticada), autenticada.token(), refreshTokens.emitir(autenticada.usuarioId()));
    }

    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Sesion refrescar(String refreshToken) {
        var rotacion = refreshTokens.rotar(refreshToken);
        var usuario = usuarios.findById(rotacion.usuarioId()).orElseThrow(SesionInvalidaException::new);
        LoginResponseDto datos = autenticacion.sesion(usuario);
        return new Sesion(sinToken(datos), datos.token(), rotacion.token());
    }

    public void cerrar(String refreshToken) {
        refreshTokens.cerrar(refreshToken);
    }

    private static LoginResponseDto sinToken(LoginResponseDto d) {
        return new LoginResponseDto(null, d.usuarioId(), d.empresaId(), d.empresaNombre(), d.nombre(), d.email(), d.rol(),
            d.superAdmin(), d.suscripcionActiva(), d.planCodigo(), d.tieneFoto(), d.obras());
    }
}
