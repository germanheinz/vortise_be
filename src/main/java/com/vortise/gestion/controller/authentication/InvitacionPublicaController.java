package com.vortise.gestion.controller.authentication;

import com.vortise.gestion.application.dto.InvitacionUsuarioDto;
import com.vortise.gestion.application.dto.InvitacionesDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.infrastructure.security.SessionCookies;
import com.vortise.gestion.application.port.IdentidadExternaVerificador;
import com.vortise.gestion.infrastructure.security.BearerToken;
import com.vortise.gestion.service.InvitacionUsuarioService;
import com.vortise.gestion.service.InvitacionesService;
import com.vortise.gestion.service.authentication.LoginConContrasenaPolicy;
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

@RestController
@RequestMapping("/api/auth")
public class InvitacionPublicaController {
    private final InvitacionesService equipo;
    private final InvitacionUsuarioService usuario;
    private final SesionService sesiones;
    private final SessionCookies cookies;
    private final IdentidadExternaVerificador verificador;
    private final LoginConContrasenaPolicy contrasena;

    public InvitacionPublicaController(InvitacionesService equipo, InvitacionUsuarioService usuario, SesionService sesiones,
            SessionCookies cookies, IdentidadExternaVerificador verificador, LoginConContrasenaPolicy contrasena) {
        this.equipo = equipo;
        this.usuario = usuario;
        this.sesiones = sesiones;
        this.cookies = cookies;
        this.verificador = verificador;
        this.contrasena = contrasena;
    }

    @PostMapping("/invitaciones/consultar")
    public InvitacionesDto.Consulta consultarEquipo(@Valid @RequestBody InvitacionesDto.Token dto) {
        return equipo.consultar(dto.token());
    }

    @PostMapping("/invitaciones/aceptar")
    public ResponseEntity<LoginResponseDto> aceptarEquipo(@Valid @RequestBody InvitacionesDto.Aceptar dto) {
        contrasena.requireEnabled();
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(equipo.aceptar(dto)));
    }

    @PostMapping("/invitaciones-usuario/consultar")
    public InvitacionUsuarioDto.Consulta consultarUsuario(@Valid @RequestBody InvitacionUsuarioDto.Token dto) {
        return usuario.consultar(dto.token());
    }

    @PostMapping("/invitaciones-usuario/aceptar")
    public ResponseEntity<LoginResponseDto> aceptarUsuario(@Valid @RequestBody InvitacionUsuarioDto.Aceptar dto) {
        contrasena.requireEnabled();
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(usuario.aceptar(dto)));
    }

    @PostMapping("/invitaciones/aceptar-clerk")
    public ResponseEntity<LoginResponseDto> aceptarEquipoConIdentidad(@Valid @RequestBody InvitacionesDto.AceptarConIdentidad dto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var identidad = verificador.verificar(BearerToken.from(authorization));
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(equipo.aceptarConIdentidad(dto, identidad)));
    }

    @PostMapping("/invitaciones-usuario/aceptar-clerk")
    public ResponseEntity<LoginResponseDto> aceptarUsuarioConIdentidad(@Valid @RequestBody InvitacionUsuarioDto.AceptarConIdentidad dto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var identidad = verificador.verificar(BearerToken.from(authorization));
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(usuario.aceptarConIdentidad(dto, identidad)));
    }
}
