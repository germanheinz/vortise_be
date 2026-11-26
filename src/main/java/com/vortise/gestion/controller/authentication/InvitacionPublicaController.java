package com.vortise.gestion.controller.authentication;

import com.vortise.gestion.application.dto.InvitacionUsuarioDto;
import com.vortise.gestion.application.dto.InvitacionesDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.infrastructure.security.SessionCookies;
import com.vortise.gestion.service.InvitacionUsuarioService;
import com.vortise.gestion.service.InvitacionesService;
import com.vortise.gestion.service.authentication.SesionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class InvitacionPublicaController {
    private final InvitacionesService equipo;
    private final InvitacionUsuarioService usuario;
    private final SesionService sesiones;
    private final SessionCookies cookies;

    @PostMapping("/invitaciones/consultar")
    public InvitacionesDto.Consulta consultarEquipo(@Valid @RequestBody InvitacionesDto.Token dto) {
        return equipo.consultar(dto.token());
    }

    @PostMapping("/invitaciones/aceptar")
    public ResponseEntity<LoginResponseDto> aceptarEquipo(@Valid @RequestBody InvitacionesDto.Aceptar dto) {
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(equipo.aceptar(dto)));
    }

    @PostMapping("/invitaciones-usuario/consultar")
    public InvitacionUsuarioDto.Consulta consultarUsuario(@Valid @RequestBody InvitacionUsuarioDto.Token dto) {
        return usuario.consultar(dto.token());
    }

    @PostMapping("/invitaciones-usuario/aceptar")
    public ResponseEntity<LoginResponseDto> aceptarUsuario(@Valid @RequestBody InvitacionUsuarioDto.Aceptar dto) {
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(usuario.aceptar(dto)));
    }

    @PostMapping("/invitaciones-usuario/aceptar-clerk")
    public ResponseEntity<LoginResponseDto> aceptarClerk(@Valid @RequestBody InvitacionUsuarioDto.AceptarClerk dto) {
        return cookies.respond(HttpStatus.CREATED, sesiones.abrir(usuario.aceptarClerk(dto)));
    }
}
