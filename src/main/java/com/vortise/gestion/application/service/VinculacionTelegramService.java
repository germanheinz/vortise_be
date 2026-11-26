package com.vortise.gestion.application.service;

import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.VinculoTelegram;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.persistence.JpaVinculoTelegramRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VinculacionTelegramService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final CurrentIdentity identidad;
    private final UsuarioEmpresaRepository usuarios;
    private final JpaVinculoTelegramRepository vinculos;

    public VinculacionTelegramService(CurrentIdentity identidad, UsuarioEmpresaRepository usuarios,
            JpaVinculoTelegramRepository vinculos) {
        this.identidad = identidad;
        this.usuarios = usuarios;
        this.vinculos = vinculos;
    }

    public Inicio iniciar() {
        long usuario = identidad.usuarioId();
        bloquearUsuario(usuario);
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expira = Instant.now().plusSeconds(600);
        var vinculo = vinculos.findById(usuario).orElseGet(() -> new VinculoTelegram(usuario));
        vinculo.iniciar(hash(codigo), expira);
        vinculos.save(vinculo);
        return new Inicio(codigo, expira);
    }

    public boolean recibir(String codigo, long chatId) {
        String hash = hash(codigo);
        var usuario = vinculos.buscarUsuarioPorCodigo(hash);
        if (usuario.isEmpty()) return false;
        bloquearUsuario(usuario.get());
        return vinculos.findByCodigoHash(hash)
            .filter(vinculo -> vinculo.aceptar(hash, chatId, Instant.now())).isPresent();
    }

    public Estado confirmar(long chatId) {
        long usuario = identidad.usuarioId();
        bloquearUsuario(usuario);
        var vinculo = vinculos.findById(usuario)
            .orElseThrow(() -> new ConflictoEstadoException("No hay una vinculación pendiente"));
        if (vinculos.findByChatConfirmado(chatId).filter(otro -> !otro.getUsuarioId().equals(usuario)).isPresent()
                || !vinculo.confirmar(chatId, Instant.now())) {
            throw new ConflictoEstadoException("La vinculación no está disponible o venció");
        }
        return new Estado(vinculo.getChatConfirmado(), null);
    }

    @Transactional(readOnly = true)
    public Estado estado() {
        return vinculos.findById(identidad.usuarioId())
            .map(v -> new Estado(v.getChatConfirmado(), v.getChatPendiente()))
            .orElseGet(() -> new Estado(null, null));
    }

    public void desvincular() {
        long usuario = identidad.usuarioId();
        bloquearUsuario(usuario);
        vinculos.deleteById(usuario);
    }

    private void bloquearUsuario(long usuario) {
        var cuenta = usuarios.bloquearPorId(usuario)
            .orElseThrow(() -> new AccessDeniedException("Cuenta no disponible"));
        if (!cuenta.isActivo() || (!cuenta.isSuperAdmin() && cuenta.getRol() != com.vortise.gestion.domain.model.RolEmpresa.ADMINISTRADOR)) {
            throw new AccessDeniedException("La vinculación requiere una cuenta administradora activa");
        }
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record Inicio(String codigo, Instant expiraEn) { }
    public record Estado(Long chatConfirmado, Long chatPendiente) { }
}
