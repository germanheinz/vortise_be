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
    public AuthController(AutenticacionUseCase autenticacionUseCase) { this.autenticacionUseCase = autenticacionUseCase; }
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) { return ResponseEntity.ok(autenticacionUseCase.login(request)); }
}
