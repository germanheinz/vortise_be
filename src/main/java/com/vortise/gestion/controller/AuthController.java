package com.vortise.gestion.controller;

import com.vortise.gestion.application.dto.ErrorResponseDto;
import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.MeDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.service.AutenticacionService;
import com.vortise.gestion.service.RegistroService;
import com.vortise.gestion.service.SesionService;
import com.vortise.gestion.infrastructure.security.SessionCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
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

    @GetMapping("/me")
    public ResponseEntity<MeDto> me() {
        return ResponseEntity.ok(autenticacion.me());
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest http) {
        return cookies.respond(HttpStatus.OK, sesiones.abrir(autenticacion.login(request, http.getRemoteAddr())));
    }

    @PostMapping("/auth/registro")
    public ResponseEntity<LoginResponseDto> registro(@Valid @RequestBody RegistroRequestDto request) {
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(registro.registrar(request)));
    }

    /** Exchanges the refresh cookie for a new access token and a new refresh token (rotation). */
    @PostMapping("/auth/refresh")
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

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        sesiones.cerrar(SessionCookies.read(http, SessionCookies.REFRESH));
        return cookies.cleared(HttpStatus.NO_CONTENT);
    }
}
