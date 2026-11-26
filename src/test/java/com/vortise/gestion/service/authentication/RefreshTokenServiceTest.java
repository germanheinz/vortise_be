package com.vortise.gestion.service.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.exception.SesionInvalidaException;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.RefreshToken;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.RefreshTokenRepository;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenServiceTest {
    private final FakeTokens tokens = new FakeTokens();
    private final UsuarioEmpresaRepository users = mock(UsuarioEmpresaRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(tokens, users);
    private UsuarioEmpresa user;

    @BeforeEach
    void setup() {
        user = new UsuarioEmpresa(new Empresa("Own", null), "u@example.invalid", "U", "hash", RolEmpresa.ADMINISTRADOR);
        ReflectionTestUtils.setField(user, "id", 5L);
        when(users.findById(5L)).thenReturn(Optional.of(user));
    }

    @Test
    void issuedTokenIsOnlyStoredAsAHash() {
        String raw = service.emitir(5L);

        assertEquals(1, tokens.rows.size());
        assertNotEquals(raw, tokens.rows.get(0).getTokenHash());
        assertEquals(PasswordResetTransactions.hash(raw), tokens.rows.get(0).getTokenHash());
    }

    @Test
    void rotationReturnsANewTokenInTheSameFamilyAndRetiresTheOldOne() {
        String first = service.emitir(5L);

        var rotated = service.rotar(first);

        assertEquals(5L, rotated.usuarioId());
        assertNotEquals(first, rotated.token());
        assertEquals(2, tokens.rows.size());
        assertEquals(tokens.rows.get(0).getFamilia(), tokens.rows.get(1).getFamilia());
        assertNotNull(tokens.rows.get(0).getUsadoEn());
        assertNotNull(service.rotar(rotated.token()).token());
    }

    @Test
    void reusingARetiredTokenAfterTheGraceRevokesTheWholeFamily() {
        String first = service.emitir(5L);
        String second = service.rotar(first).token();
        tokens.rows.get(0).marcarUsado(Instant.now().minusSeconds(RefreshTokenService.GRACIA_SEGUNDOS + 5));

        assertThrows(SesionInvalidaException.class, () -> service.rotar(first));

        assertTrue(tokens.rows.stream().allMatch(row -> row.getRevocadoEn() != null));
        assertThrows(SesionInvalidaException.class, () -> service.rotar(second));
    }

    @Test
    void aConcurrentSecondUseWithinTheGraceDoesNotLogTheUserOut() {
        String first = service.emitir(5L);
        service.rotar(first);

        assertNotNull(service.rotar(first).token());
        assertTrue(tokens.rows.stream().noneMatch(row -> row.getRevocadoEn() != null));
    }

    @Test
    void unknownBlankAndExpiredTokensAreRejected() {
        assertThrows(SesionInvalidaException.class, () -> service.rotar("not-a-token"));
        assertThrows(SesionInvalidaException.class, () -> service.rotar(null));
        assertThrows(SesionInvalidaException.class, () -> service.rotar("  "));
        String raw = service.emitir(5L);
        ReflectionTestUtils.setField(tokens.rows.get(0), "venceEn", Instant.now().minusSeconds(1));

        assertThrows(SesionInvalidaException.class, () -> service.rotar(raw));
    }

    @Test
    void aSessionVersionChangeEndsTheChain() {
        String raw = service.emitir(5L);
        user.setRol(RolEmpresa.CONSULTA);

        assertThrows(SesionInvalidaException.class, () -> service.rotar(raw));
        assertTrue(tokens.rows.get(0).getRevocadoEn() != null);
    }

    @Test
    void anInactiveAccountCannotRefresh() {
        String raw = service.emitir(5L);
        user.setActivo(false);

        assertThrows(SesionInvalidaException.class, () -> service.rotar(raw));
    }

    @Test
    void aChainOlderThanTheAbsoluteLimitCannotBeRenewed() {
        String raw = service.emitir(5L);
        ReflectionTestUtils.setField(tokens.rows.get(0), "inicioFamilia", Instant.now().minusSeconds(RefreshTokenService.MAX_FAMILIA_SEGUNDOS + 5));

        assertThrows(SesionInvalidaException.class, () -> service.rotar(raw));
    }

    @Test
    void aRotatedTokenNeverOutlivesTheFamilyLimit() {
        String raw = service.emitir(5L);
        Instant start = Instant.now().minusSeconds(RefreshTokenService.MAX_FAMILIA_SEGUNDOS - 100);
        ReflectionTestUtils.setField(tokens.rows.get(0), "inicioFamilia", start);

        service.rotar(raw);

        assertFalse(tokens.rows.get(1).getVenceEn().isAfter(start.plusSeconds(RefreshTokenService.MAX_FAMILIA_SEGUNDOS)));
    }

    @Test
    void logoutRevokesTheFamilyAndIgnoresUnknownTokens() {
        String raw = service.emitir(5L);
        service.cerrar("unknown");
        service.cerrar(null);

        service.cerrar(raw);

        assertThrows(SesionInvalidaException.class, () -> service.rotar(raw));
    }

    private static final class FakeTokens implements RefreshTokenRepository {
        final List<RefreshToken> rows = new ArrayList<>();
        @Override public RefreshToken save(RefreshToken token) { if (!rows.contains(token)) rows.add(token); return token; }
        @Override public Optional<RefreshToken> bloquearPorHash(String hash) { return rows.stream().filter(r -> r.getTokenHash().equals(hash)).findFirst(); }
        @Override public int revocarFamilia(String familia, Instant ahora) {
            rows.stream().filter(r -> r.getFamilia().equals(familia)).forEach(r -> r.revocar(ahora)); return 0;
        }
        @Override public int revocarPorUsuario(Long usuarioId, Instant ahora) {
            rows.stream().filter(r -> r.getUsuarioId().equals(usuarioId)).forEach(r -> r.revocar(ahora)); return 0;
        }
        @Override public int limpiarVencidos(Instant antes) { rows.removeIf(r -> r.getVenceEn().isBefore(antes)); return 0; }
    }
}
