package com.vortise.gestion.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vinculos_telegram")
public class VinculoTelegram {
    @Id
    private Long usuarioId;
    @Column(length = 64, unique = true)
    private String codigoHash;
    private Instant expiraEn;
    private Long chatPendiente;
    @Column(unique = true)
    private Long chatConfirmado;

    protected VinculoTelegram() { }

    public VinculoTelegram(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getUsuarioId() { return usuarioId; }
    public Long getChatPendiente() { return chatPendiente; }
    public Long getChatConfirmado() { return chatConfirmado; }

    public void iniciar(String hash, Instant expira) {
        codigoHash = hash;
        expiraEn = expira;
        chatPendiente = null;
    }

    public boolean aceptar(String hash, long chatId, Instant ahora) {
        if (!hash.equals(codigoHash) || expiraEn == null || !ahora.isBefore(expiraEn)) return false;
        codigoHash = null;
        chatPendiente = chatId;
        return true;
    }

    public boolean confirmar(long chatId, Instant ahora) {
        if (!Long.valueOf(chatId).equals(chatPendiente) || expiraEn == null || !ahora.isBefore(expiraEn)) return false;
        chatConfirmado = chatId;
        chatPendiente = null;
        expiraEn = null;
        return true;
    }
}
