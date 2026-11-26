package com.vortise.gestion.service.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ClerkUserSyncServiceTest {
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final ClerkUserSyncService sync = new ClerkUserSyncService(usuarios);

    ClerkUserSyncServiceTest() {
        ReflectionTestUtils.setField(sync, "clerkEnabled", true);
    }

    @Test
    void anUnknownEmailNeverGetsAnAccount() {
        assertNull(sync.sincronizarDesdeClerk("user_new", "stranger@example.invalid"));
        assertNull(sync.sincronizarDesdeClerk("user_new", null));
        verify(usuarios, never()).save(any());
    }

    @Test
    void linksAnExistingUserByVerifiedEmailButNeverStealsAnotherLink() {
        var free = new UsuarioEmpresa(new Empresa("E", null), "ana@example.invalid", "Ana", "hash", RolEmpresa.CONSULTA);
        when(usuarios.findByEmail("ana@example.invalid")).thenReturn(Optional.of(free));
        assertEquals(free, sync.sincronizarDesdeClerk("user_ana", " Ana@Example.invalid "));
        assertEquals("user_ana", free.getClerkUserId());

        var linked = new UsuarioEmpresa(new Empresa("E", null), "bob@example.invalid", "Bob", "hash", RolEmpresa.CONSULTA);
        linked.setClerkUserId("user_bob");
        when(usuarios.findByEmail("bob@example.invalid")).thenReturn(Optional.of(linked));
        assertNull(sync.sincronizarDesdeClerk("user_attacker", "bob@example.invalid"));
        assertEquals("user_bob", linked.getClerkUserId());
    }
}
