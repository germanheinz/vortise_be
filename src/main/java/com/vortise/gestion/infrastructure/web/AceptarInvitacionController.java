package com.vortise.gestion.infrastructure.web;
import com.vortise.gestion.application.dto.InvitacionesDto.*;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.service.InvitacionesUseCase;
import com.vortise.gestion.application.service.SesionUseCase;
import com.vortise.gestion.infrastructure.security.SessionCookies;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth/invitaciones")
public class AceptarInvitacionController {
    private final InvitacionesUseCase useCase;
    private final SesionUseCase sesiones;
    private final SessionCookies cookies;
    public AceptarInvitacionController(InvitacionesUseCase useCase, SesionUseCase sesiones, SessionCookies cookies) {
        this.useCase = useCase; this.sesiones = sesiones; this.cookies = cookies;
    }
    @PostMapping("/consultar") public Consulta consultar(@Valid @RequestBody Token dto) { return useCase.consultar(dto.token()); }
    @PostMapping("/aceptar") public ResponseEntity<LoginResponseDto> aceptar(@Valid @RequestBody Aceptar dto) { return cookies.respond(HttpStatus.CREATED, sesiones.abrir(useCase.aceptar(dto))); }
}
