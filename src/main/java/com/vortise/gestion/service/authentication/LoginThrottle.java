package com.vortise.gestion.service.authentication;

import com.vortise.gestion.domain.model.PasswordResetQuota;
import com.vortise.gestion.domain.repository.PasswordResetQuotaRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limits failed logins per account and per client address in a fixed window. Counters live in the
 * shared quota table (hashed keys, no personal data) and are written in their own transaction so a
 * failed login, which rolls back its caller, still records the attempt.
 */
@Service
public class LoginThrottle {
    static final long VENTANA_SEGUNDOS = 900;
    static final int MAX_FALLOS_POR_CUENTA = 5;
    // Behind a reverse proxy every user may share one address, so this limit stays generous.
    static final int MAX_FALLOS_POR_IP = 50;
    private static final long RETENCION_SEGUNDOS = 7200;

    private final PasswordResetQuotaRepository quotas;

    public LoginThrottle(PasswordResetQuotaRepository quotas) {
        this.quotas = quotas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean bloqueado(String email, String ip) {
        Instant now = Instant.now();
        return excedido(claveCuenta(email), now, MAX_FALLOS_POR_CUENTA) || excedido(claveIp(ip), now, MAX_FALLOS_POR_IP);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String email, String ip) {
        Instant now = Instant.now();
        quotas.limpiar(now.minusSeconds(RETENCION_SEGUNDOS));
        registrar(claveCuenta(email), now, MAX_FALLOS_POR_CUENTA);
        registrar(claveIp(ip), now, MAX_FALLOS_POR_IP);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void limpiarCuenta(String email) {
        quotas.findById(claveCuenta(email)).ifPresent(quota -> {
            quota.reiniciar(Instant.now());
            quotas.save(quota);
        });
    }

    private boolean excedido(String clave, Instant now, int limite) {
        return quotas.findById(clave).map(quota -> !quota.disponible(now, VENTANA_SEGUNDOS, limite)).orElse(false);
    }

    private void registrar(String clave, Instant now, int limite) {
        PasswordResetQuota quota = quotas.findById(clave).orElseGet(() -> new PasswordResetQuota(clave, now));
        quota.disponible(now, VENTANA_SEGUNDOS, limite);
        quota.registrar();
        quotas.save(quota);
    }

    // "lc:" / "li:" + SHA-256 hex stays within the 72-character key column and is distinct from password reset keys.
    private static String claveCuenta(String email) {
        return "lc:" + PasswordResetTransactions.hash(email);
    }

    private static String claveIp(String ip) {
        return "li:" + PasswordResetTransactions.hash(ip == null || ip.isBlank() ? "unknown" : ip);
    }
}
