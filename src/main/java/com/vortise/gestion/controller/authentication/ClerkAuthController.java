package com.vortise.gestion.controller.authentication;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroConIdentidadDto;
import com.vortise.gestion.application.port.IdentidadExternaVerificador;
import com.vortise.gestion.infrastructure.security.BearerToken;
import com.vortise.gestion.infrastructure.security.SessionCookies;
import com.vortise.gestion.service.authentication.IdentidadExternaService;
import com.vortise.gestion.service.authentication.RegistroService;
import com.vortise.gestion.service.authentication.SesionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exchanges a verified external identity for the application's cookie session. */
@RestController
@RequestMapping("/api/auth/clerk")
public class ClerkAuthController {
    private final IdentidadExternaVerificador verificador;
    private final IdentidadExternaService identidades;
    private final RegistroService registro;
    private final SesionService sesiones;
    private final SessionCookies cookies;

    public ClerkAuthController(IdentidadExternaVerificador verificador, IdentidadExternaService identidades,
            RegistroService registro, SesionService sesiones, SessionCookies cookies) {
        this.verificador = verificador;
        this.identidades = identidades;
        this.registro = registro;
        this.sesiones = sesiones;
        this.cookies = cookies;
    }

    @PostMapping("/sesion")
    public ResponseEntity<LoginResponseDto> iniciarSesion(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var identidad = verificador.verificar(BearerToken.from(authorization));
        return cookies.respond(HttpStatus.OK, sesiones.abrir(identidades.iniciarSesion(identidad)));
    }

    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDto> registrar(@Valid @RequestBody RegistroConIdentidadDto request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var identidad = verificador.verificar(BearerToken.from(authorization));
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(registro.registrarConIdentidad(request, identidad)));
    }
}
