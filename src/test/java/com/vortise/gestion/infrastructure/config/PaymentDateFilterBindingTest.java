package com.vortise.gestion.infrastructure.config;

import static org.mockito.Mockito.*;

import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class PaymentDateFilterBindingTest {
    @Test
    void absentDateBoundsNeverBindNullAndPresentBoundsArePreserved() {
        var repository = mock(PagoSuscripcionRepository.class, CALLS_REAL_METHODS);
        var page = PageRequest.of(0, 20);
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        Instant end = Instant.parse("2026-10-28T00:00:00Z");
        repository.buscarSuperAdmin(null, null, null, null, page);
        verify(repository).buscarSuperAdminConFechas(false, 0L, false, "", false, Instant.EPOCH, false, Instant.EPOCH, page);
        repository.buscarSuperAdmin(1L, null, null, null, page);
        verify(repository).buscarSuperAdminConFechas(true, 1L, false, "", false, Instant.EPOCH, false, Instant.EPOCH, page);
        repository.buscarSuperAdmin(1L, "PAGADO", start, end, page);
        verify(repository).buscarSuperAdminConFechas(true, 1L, true, "PAGADO", true, start, true, end, page);
        repository.buscarSuperAdmin(1L, null, start, null, page);
        verify(repository).buscarSuperAdminConFechas(true, 1L, false, "", true, start, false, Instant.EPOCH, page);
        repository.buscarSuperAdmin(1L, null, null, end, page);
        verify(repository).buscarSuperAdminConFechas(true, 1L, false, "", false, Instant.EPOCH, true, end, page);
    }
}
