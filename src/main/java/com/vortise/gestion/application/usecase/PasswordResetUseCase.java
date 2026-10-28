package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.port.PasswordMailGateway;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetUseCase {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetUseCase.class);
    private final PasswordResetTransactions transactions;
    private final PasswordMailGateway mail;
    private final String frontend;
    public PasswordResetUseCase(PasswordResetTransactions transactions, PasswordMailGateway mail,
            @Value("${app.frontend-url}") String frontend) {
        this.transactions = transactions; this.mail = mail; this.frontend = frontend.replaceAll("/+$", "");
    }
    public void request(String email, String ip) {
        if (!mail.enabled()) return;
        try {
            var delivery = transactions.issue(email.trim().toLowerCase(Locale.ROOT), ip == null ? "unknown" : ip);
            delivery.ifPresent(value -> mail.sendReset(value.email(), frontend + "/restablecer-password#token=" + value.token()));
        } catch (RuntimeException ex) {
            // No exception messages/addresses/tokens: neither response nor logs reveal account existence.
            log.warn("Password recovery request could not be delivered");
        }
    }
    public void confirm(String token, String password) {
        String email = transactions.confirm(token, password);
        try { mail.sendChanged(email); }
        catch (RuntimeException ex) { log.warn("Password change notification could not be delivered"); }
    }
}
