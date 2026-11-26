package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.ErrorResponseDto;
import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.application.service.AutenticacionUseCase;
import com.vortise.gestion.application.service.RegistroUseCase;
import com.vortise.gestion.application.service.SesionUseCase;
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
    private final AutenticacionUseCase autenticacionUseCase;
    private final RegistroUseCase registroUseCase;
    private final SesionUseCase sesionUseCase;
    private final SessionCookies cookies;

    public AuthController(AutenticacionUseCase autenticacionUseCase, RegistroUseCase registroUseCase,
            SesionUseCase sesionUseCase, SessionCookies cookies) {
        this.autenticacionUseCase = autenticacionUseCase; this.registroUseCase = registroUseCase;
        this.sesionUseCase = sesionUseCase; this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest http) {
        return cookies.respond(HttpStatus.OK, sesionUseCase.abrir(autenticacionUseCase.login(request, http.getRemoteAddr())));
    }

    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDto> registro(@Valid @RequestBody RegistroRequestDto request) {
        return cookies.respond(HttpStatus.CREATED, sesionUseCase.abrir(registroUseCase.registrar(request)));
    }

    /** Exchanges the refresh cookie for a new access token and a new refresh token (rotation). */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest http) {
        try {
            return cookies.respond(HttpStatus.OK, sesionUseCase.refrescar(SessionCookies.read(http, SessionCookies.REFRESH)));
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
        sesionUseCase.cerrar(SessionCookies.read(http, SessionCookies.REFRESH));
        return cookies.cleared(HttpStatus.NO_CONTENT);
    }
}
