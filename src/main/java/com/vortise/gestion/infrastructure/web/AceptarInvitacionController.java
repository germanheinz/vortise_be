package com.vortise.gestion.infrastructure.web;
import com.vortise.gestion.application.dto.InvitacionesDto.*;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.usecase.InvitacionesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth/invitaciones")
public class AceptarInvitacionController {
    private final InvitacionesUseCase useCase;
    public AceptarInvitacionController(InvitacionesUseCase useCase) { this.useCase = useCase; }
    @PostMapping("/consultar") public Consulta consultar(@Valid @RequestBody Token dto) { return useCase.consultar(dto.token()); }
    @PostMapping("/aceptar") public ResponseEntity<LoginResponseDto> aceptar(@Valid @RequestBody Aceptar dto) { return ResponseEntity.status(201).body(useCase.aceptar(dto)); }
}
