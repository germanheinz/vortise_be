package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ErrorResponseDto;
import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.application.service.AutenticacionService;
import com.vortise.gestion.application.service.RegistroService;
import com.vortise.gestion.application.service.SesionService;
import com.vortise.gestion.infrastructure.security.SessionCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AutenticacionService autenticacion;
    private final RegistroService registro;
    private final SesionService sesiones;
    private final SessionCookies cookies;

    public AuthController(AutenticacionService autenticacion, RegistroService registro,
            SesionService sesiones, SessionCookies cookies) {
        this.autenticacion = autenticacion; this.registro = registro;
        this.sesiones = sesiones; this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest http) {
        return cookies.respond(HttpStatus.OK, sesiones.abrir(autenticacion.login(request, http.getRemoteAddr())));
    }

    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDto> registro(@Valid @RequestBody RegistroRequestDto request) {
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(registro.registrar(request)));
    }

    /** Exchanges the refresh cookie for a new access token and a new refresh token (rotation). */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest http) {
        try {
            return cookies.respond(HttpStatus.OK, sesiones.refrescar(SessionCookies.read(http, SessionCookies.REFRESH)));
        } catch (SesionInvalidaException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponseDto(401, "Unauthorized", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        sesiones.cerrar(SessionCookies.read(http, SessionCookies.REFRESH));
        return cookies.cleared(HttpStatus.NO_CONTENT);
    }
}
