package com.vortise.gestion.service.authentication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vortise.gestion.domain.model.PasswordResetQuota;
import com.vortise.gestion.domain.repository.PasswordResetQuotaRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {
    private FakeQuotas quotas;
    private LoginThrottle throttle;

    @BeforeEach
    void setUp() {
        quotas = new FakeQuotas();
        throttle = new LoginThrottle(quotas);
    }

    @Test
    void nothingIsBlockedBeforeAnyFailure() {
        assertFalse(throttle.bloqueado("a@example.invalid", "203.0.113.1"));
    }

    @Test
    void accountIsBlockedAfterTheLimitRegardlessOfAddressAndOtherAccountsAreUnaffected() {
        for (int i = 0; i < LoginThrottle.MAX_FALLOS_POR_CUENTA; i++) throttle.registrarFallo("a@example.invalid", "203.0.113." + i);

        assertTrue(throttle.bloqueado("a@example.invalid", "198.51.100.9"));
        assertFalse(throttle.bloqueado("b@example.invalid", "198.51.100.9"));
    }

    @Test
    void oneFailureShortOfTheLimitIsNotBlocked() {
        for (int i = 0; i < LoginThrottle.MAX_FALLOS_POR_CUENTA - 1; i++) throttle.registrarFallo("a@example.invalid", "203.0.113.1");

        assertFalse(throttle.bloqueado("a@example.invalid", "198.51.100.9"));
    }

    @Test
    void addressIsBlockedAfterTheLimitAcrossDifferentAccounts() {
        for (int i = 0; i < LoginThrottle.MAX_FALLOS_POR_IP; i++) throttle.registrarFallo("user" + i + "@example.invalid", "203.0.113.1");

        assertTrue(throttle.bloqueado("nuevo@example.invalid", "203.0.113.1"));
        assertFalse(throttle.bloqueado("nuevo@example.invalid", "203.0.113.2"));
    }

    @Test
    void successfulLoginResetsTheAccountCounter() {
        for (int i = 0; i < LoginThrottle.MAX_FALLOS_POR_CUENTA - 1; i++) throttle.registrarFallo("a@example.invalid", "203.0.113.1");
        throttle.limpiarCuenta("a@example.invalid");
        for (int i = 0; i < LoginThrottle.MAX_FALLOS_POR_CUENTA - 1; i++) throttle.registrarFallo("a@example.invalid", "203.0.113.1");

        assertFalse(throttle.bloqueado("a@example.invalid", "198.51.100.9"));
    }

    @Test
    void counterExpiresAfterTheWindow() {
        var expired = new PasswordResetQuota("ignored", Instant.now().minusSeconds(LoginThrottle.VENTANA_SEGUNDOS + 5));
        for (int i = 0; i < LoginThrottle.MAX_FALLOS_POR_CUENTA; i++) expired.registrar();
        quotas.rows.put("lc:" + PasswordResetTransactions.hash("a@example.invalid"), expired);

        assertFalse(throttle.bloqueado("a@example.invalid", "203.0.113.1"));
    }

    @Test
    void storedKeysAreHashedAndFitTheKeyColumn() {
        throttle.registrarFallo("persona@example.invalid", "203.0.113.77");

        assertFalse(quotas.rows.isEmpty());
        for (String key : quotas.rows.keySet()) {
            assertTrue(key.length() <= 72, key);
            assertFalse(key.contains("persona") || key.contains("203.0.113"), key);
        }
    }

    @Test
    void missingAddressIsGroupedUnderOneKeyInsteadOfFailing() {
        throttle.registrarFallo("a@example.invalid", null);

        assertFalse(throttle.bloqueado("b@example.invalid", null));
    }

    private static final class FakeQuotas implements PasswordResetQuotaRepository {
        final Map<String, PasswordResetQuota> rows = new HashMap<>();
        private String lastKey;

        @Override public Optional<PasswordResetQuota> findById(String key) { lastKey = key; return Optional.ofNullable(rows.get(key)); }
        @Override public PasswordResetQuota save(PasswordResetQuota quota) { rows.put(lastKey, quota); return quota; }
        @Override public Optional<PasswordResetQuota> bloquearGuard() { return Optional.empty(); }
        @Override public void limpiar(Instant antes) { }
    }
}
