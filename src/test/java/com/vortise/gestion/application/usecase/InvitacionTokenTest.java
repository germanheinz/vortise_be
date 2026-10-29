package com.vortise.gestion.application.usecase;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import com.vortise.gestion.domain.model.InvitacionEquipo;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import static org.junit.jupiter.api.Assertions.*;
class InvitacionTokenTest {
    @Test void tokenHasExpectedEntropyShapeAndOnlyHashIsPersisted() {
        String a = InvitacionToken.nuevo(), b = InvitacionToken.nuevo();
        assertTrue(a.matches("[A-Za-z0-9_-]{43}")); assertNotEquals(a, b);
        assertEquals(64, InvitacionToken.hash(a).length()); assertNotEquals(a, InvitacionToken.hash(a));
        assertThrows(SolicitudInvalidaException.class, () -> InvitacionToken.hash("invalid"));
    }
    @Test void oneInvitationCoversSeveralCompaniesAndExpiresAtBoundary() {
        Instant now = Instant.parse("2026-10-29T10:00:00Z");
        var invitation = new InvitacionEquipo(InvitacionToken.hash(InvitacionToken.nuevo()), 1L, Set.of(2L, 3L), now);
        assertEquals(Set.of(2L, 3L), invitation.getEmpresaIds());
        assertTrue(invitation.disponible(now)); assertFalse(invitation.disponible(now.plusSeconds(72 * 3600)));
        invitation.consumir(now); assertFalse(invitation.disponible(now)); assertEquals("ACEPTADA", invitation.estado(now));
        var revoked = new InvitacionEquipo("hash", 1L, Set.of(2L), now);
        revoked.revocar(now); assertFalse(revoked.disponible(now)); assertEquals("REVOCADA", revoked.estado(now));
    }
}
