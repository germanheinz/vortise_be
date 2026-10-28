package com.vortise.gestion.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_quotas")
public class PasswordResetQuota {
    @Id @Column(length = 72) private String clave;
    @Column(nullable = false) private Instant inicio;
    @Column(nullable = false) private int solicitudes;
    protected PasswordResetQuota() { }
    public PasswordResetQuota(String clave, Instant now) { this.clave = clave; inicio = now; }
    public boolean disponible(Instant now, long segundos, int limite) {
        if (!inicio.plusSeconds(segundos).isAfter(now)) { inicio = now; solicitudes = 0; }
        return solicitudes < limite;
    }
    public void registrar() { solicitudes++; }
}
