package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.dto.LoginRequestDto;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.usecase.AutenticacionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AutenticacionUseCase autenticacionUseCase;
    private final com.vortise.gestion.application.usecase.RegistroUseCase registroUseCase;
    public AuthController(AutenticacionUseCase autenticacionUseCase, com.vortise.gestion.application.usecase.RegistroUseCase registroUseCase) {
        this.autenticacionUseCase = autenticacionUseCase; this.registroUseCase = registroUseCase;
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) { return ResponseEntity.ok(autenticacionUseCase.login(request)); }
    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDto> registro(@Valid @RequestBody com.vortise.gestion.application.dto.RegistroRequestDto request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(registroUseCase.registrar(request));
    }
}
