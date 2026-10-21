package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.StripeWebhookUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeWebhookController {
    private final StripeWebhookUseCase useCase;

    public StripeWebhookController(StripeWebhookUseCase useCase) { this.useCase = useCase; }

    /** Raw body is required: signature verification fails on re-serialized JSON. */
    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        useCase.procesar(payload, signature);
        return ResponseEntity.ok().build();
    }
}
