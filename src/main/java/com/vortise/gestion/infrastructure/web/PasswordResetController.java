package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.PasswordResetUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetUseCase useCase;
    public PasswordResetController(PasswordResetUseCase useCase) { this.useCase = useCase; }
    public record Request(@Email @NotBlank @Size(max = 254) String email) {
        @Override public String toString() { return "PasswordResetRequest[redacted]"; }
    }
    public record Confirm(@NotBlank @Size(max = 128) String token, @NotBlank @Size(min = 8, max = 72) String newPassword) {
        @Override public String toString() { return "PasswordResetConfirm[redacted]"; }
    }
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody Request body, HttpServletRequest request) {
        useCase.request(body.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().header("Cache-Control", "no-store").body(Map.of("message", "Si la cuenta está disponible, recibirás un enlace para recuperar tu contraseña."));
    }
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody Confirm body) {
        useCase.confirm(body.token(), body.newPassword());
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
    @ExceptionHandler({org.springframework.dao.DataAccessException.class, org.springframework.transaction.TransactionException.class})
    public ResponseEntity<Map<String, String>> storageFailure(RuntimeException exception) {
        return ResponseEntity.status(503).header("Cache-Control", "no-store")
            .body(Map.of("message", "No pudimos completar el cambio. Intentá nuevamente en unos minutos."));
    }
}
