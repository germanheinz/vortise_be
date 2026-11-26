package com.vortise.gestion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.InvitacionUsuarioDto.Aceptar;
import com.vortise.gestion.application.dto.InvitacionUsuarioDto.Crear;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.InvitacionUsuario;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.InvitacionUsuarioRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class InvitacionUsuarioUseCaseTest {
    private static final long COMPANY = 1L;
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AutenticacionUseCase autenticacion = mock(AutenticacionUseCase.class);
    private final List<InvitacionUsuario> stored = new ArrayList<>();
    private final InvitacionUsuarioRepository invitaciones = new FakeInvitaciones(stored);
    private final InvitacionUsuarioUseCase useCase = new InvitacionUsuarioUseCase(empresas, usuarios, invitaciones, proyectos,
        identity, encoder, autenticacion, "https://app.example.invalid/");
    private Empresa empresa;

    @BeforeEach
    void setup() {
        empresa = new Empresa("Constructora", null);
        ReflectionTestUtils.setField(empresa, "id", COMPANY);
        when(empresas.bloquearPorId(COMPANY)).thenReturn(Optional.of(empresa));
        when(empresas.findById(COMPANY)).thenReturn(Optional.of(empresa));
        when(identity.usuarioId()).thenReturn(9L);
        when(encoder.encode(any())).thenReturn("encoded");
        when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var sesion = new LoginResponseDto(null, 1L, COMPANY, "Constructora", "N", "n@example.invalid", "CONSULTA", false, false, null, false, List.of());
        when(autenticacion.sesion(any())).thenReturn(sesion);
    }

    private void site(long id, long company) {
        var owner = new Empresa("Owner", null);
        ReflectionTestUtils.setField(owner, "id", company);
        var project = new Proyecto();
        project.setId(id);
        project.setEmpresaRelacionada(owner);
        when(proyectos.findById(id)).thenReturn(Optional.of(project));
    }

    private static String token(String link) { return link.substring(link.indexOf("#token=") + 7); }

    @Test
    void createsAOneTimeLinkAndStoresOnlyTheHashOfTheToken() {
        var created = useCase.crear(COMPANY, new Crear("  Ana@Example.INVALID ", RolEmpresa.CONSULTA, null));

        assertTrue(created.enlace().startsWith("https://app.example.invalid/invitacion-usuario#token="));
        String token = token(created.enlace());
        assertEquals(43, token.length());
        assertEquals("ana@example.invalid", created.email());
        assertEquals(1, stored.size());
        assertNotEquals(token, ReflectionTestUtils.getField(stored.get(0), "tokenHash"));
        assertEquals(RolEmpresa.CONSULTA, stored.get(0).getRol());
    }

    @Test
    void canInviteAdministratorJefeAndConsultaButNotOtherRoles() {
        site(7L, COMPANY);
        useCase.crear(COMPANY, new Crear("admin@example.invalid", RolEmpresa.ADMINISTRADOR, null));
        useCase.crear(COMPANY, new Crear("jefe@example.invalid", RolEmpresa.JEFE_DE_OBRA, List.of(7L)));
        useCase.crear(COMPANY, new Crear("consulta@example.invalid", RolEmpresa.CONSULTA, null));

        for (RolEmpresa denied : List.of(RolEmpresa.COLABORADOR, RolEmpresa.RESPONSABLE)) {
            assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(COMPANY, new Crear("x@example.invalid", denied, null)));
        }
        assertEquals(3, stored.size());
    }

    @Test
    void sitesAreOnlyForAJefeAndMustBelongToTheCompany() {
        site(7L, COMPANY); site(8L, 99L);
        when(proyectos.findById(404L)).thenReturn(Optional.empty());

        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(COMPANY, new Crear("a@example.invalid", RolEmpresa.CONSULTA, List.of(7L))));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(COMPANY, new Crear("b@example.invalid", RolEmpresa.JEFE_DE_OBRA, List.of(8L))));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(COMPANY, new Crear("c@example.invalid", RolEmpresa.JEFE_DE_OBRA, List.of(404L))));
        assertTrue(stored.isEmpty());
    }

    @Test
    void rejectsAnExistingUserAndADuplicatePendingInvitation() {
        var existing = mock(UsuarioEmpresa.class);
        when(usuarios.findByEmail("taken@example.invalid")).thenReturn(Optional.of(existing));

        assertThrows(ConflictoEstadoException.class, () -> useCase.crear(COMPANY, new Crear("taken@example.invalid", RolEmpresa.CONSULTA, null)));
        useCase.crear(COMPANY, new Crear("dup@example.invalid", RolEmpresa.CONSULTA, null));
        assertThrows(ConflictoEstadoException.class, () -> useCase.crear(COMPANY, new Crear("DUP@example.invalid", RolEmpresa.JEFE_DE_OBRA, null)));
    }

    @Test
    void theCompanyLimitCountsActiveUsersAndPendingInvitations() {
        when(usuarios.countByEmpresaIdAndActivoTrue(COMPANY)).thenReturn(3L);
        useCase.crear(COMPANY, new Crear("one@example.invalid", RolEmpresa.CONSULTA, null));
        useCase.crear(COMPANY, new Crear("two@example.invalid", RolEmpresa.CONSULTA, null));

        assertThrows(ConflictoEstadoException.class, () -> useCase.crear(COMPANY, new Crear("three@example.invalid", RolEmpresa.CONSULTA, null)));
    }

    @Test
    void aCallerWithoutAccessToTheCompanyCannotInviteListOrRevoke() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("no")).when(identity).requireEmpresa(2L);

        assertThrows(AccessDeniedException.class, () -> useCase.crear(2L, new Crear("a@example.invalid", RolEmpresa.CONSULTA, null)));
        assertThrows(AccessDeniedException.class, () -> useCase.listar(2L));
        assertThrows(AccessDeniedException.class, () -> useCase.revocar(2L, 1L));
    }

    @Test
    void acceptingCreatesTheUserWithTheInvitedRoleAndOnlyStillValidSites() {
        site(7L, COMPANY); site(8L, COMPANY);
        String link = useCase.crear(COMPANY, new Crear("jefe@example.invalid", RolEmpresa.JEFE_DE_OBRA, List.of(7L, 8L))).enlace();
        site(8L, 99L); // site 8 moved to another company after the invitation

        useCase.aceptar(new Aceptar(token(link), "  Juan Jefe ", "synthetic-pass-1"));

        ArgumentCaptor<UsuarioEmpresa> saved = ArgumentCaptor.forClass(UsuarioEmpresa.class);
        verify(usuarios).save(saved.capture());
        assertEquals(RolEmpresa.JEFE_DE_OBRA, saved.getValue().getRol());
        assertEquals("jefe@example.invalid", saved.getValue().getEmail());
        assertEquals("Juan Jefe", saved.getValue().getNombre());
        assertEquals(Set.of(7L), saved.getValue().getObrasAsignadas());
        assertEquals("ACEPTADA", stored.get(0).estado(Instant.now()));
    }

    @Test
    void theLinkWorksOnlyOnce() {
        String link = useCase.crear(COMPANY, new Crear("once@example.invalid", RolEmpresa.CONSULTA, null)).enlace();
        useCase.aceptar(new Aceptar(token(link), "Una Vez", "synthetic-pass-1"));

        assertThrows(SolicitudInvalidaException.class, () -> useCase.aceptar(new Aceptar(token(link), "Otra", "synthetic-pass-1")));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.consultar(token(link)));
    }

    @Test
    void revokedExpiredAndMalformedTokensAreNotAvailable() {
        var created = useCase.crear(COMPANY, new Crear("rev@example.invalid", RolEmpresa.CONSULTA, null));
        useCase.revocar(COMPANY, created.id());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.aceptar(new Aceptar(token(created.enlace()), "X", "synthetic-pass-1")));

        String other = useCase.crear(COMPANY, new Crear("exp@example.invalid", RolEmpresa.CONSULTA, null)).enlace();
        ReflectionTestUtils.setField(stored.get(1), "venceEn", Instant.now().minusSeconds(1));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.consultar(token(other)));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.consultar("not-a-token"));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.consultar(null));
    }

    @Test
    void aWeakPasswordOrATakenEmailLeavesTheInvitationUsable() {
        String link = useCase.crear(COMPANY, new Crear("weak@example.invalid", RolEmpresa.CONSULTA, null)).enlace();

        assertThrows(SolicitudInvalidaException.class, () -> useCase.aceptar(new Aceptar(token(link), "X", "short")));
        var existing = mock(UsuarioEmpresa.class);
        when(usuarios.findByEmail("weak@example.invalid")).thenReturn(Optional.of(existing));
        assertThrows(ConflictoEstadoException.class, () -> useCase.aceptar(new Aceptar(token(link), "X", "synthetic-pass-1")));

        assertEquals("PENDIENTE", stored.get(0).estado(Instant.now()));
        verify(usuarios, never()).save(any());
    }

    @Test
    void theLimitIsRecheckedWhenTheInvitationIsAccepted() {
        String link = useCase.crear(COMPANY, new Crear("late@example.invalid", RolEmpresa.CONSULTA, null)).enlace();
        when(usuarios.countByEmpresaIdAndActivoTrue(COMPANY)).thenReturn(5L);

        assertThrows(ConflictoEstadoException.class, () -> useCase.aceptar(new Aceptar(token(link), "X", "synthetic-pass-1")));
    }

    @Test
    void anInvitationOfAnotherCompanyCannotBeRevokedThroughThisOne() {
        var created = useCase.crear(COMPANY, new Crear("own@example.invalid", RolEmpresa.CONSULTA, null));

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.revocar(2L, created.id()));
        assertFalse("REVOCADA".equals(stored.get(0).estado(Instant.now())));
    }

    @Test
    void consultReturnsTheCompanyEmailAndRoleWithoutExposingAnythingElse() {
        String link = useCase.crear(COMPANY, new Crear("who@example.invalid", RolEmpresa.ADMINISTRADOR, null)).enlace();

        var consulta = useCase.consultar(token(link));

        assertEquals("Constructora", consulta.empresaNombre());
        assertEquals("who@example.invalid", consulta.email());
        assertEquals(RolEmpresa.ADMINISTRADOR, consulta.rol());
    }

    private static final class FakeInvitaciones implements InvitacionUsuarioRepository {
        private final List<InvitacionUsuario> rows;
        FakeInvitaciones(List<InvitacionUsuario> rows) { this.rows = rows; }
        @Override public InvitacionUsuario save(InvitacionUsuario i) {
            if (!rows.contains(i)) { ReflectionTestUtils.setField(i, "id", (long) rows.size() + 1); rows.add(i); }
            return i;
        }
        @Override public Optional<InvitacionUsuario> findByTokenHash(String hash) {
            return rows.stream().filter(r -> hash.equals(ReflectionTestUtils.getField(r, "tokenHash"))).findFirst();
        }
        @Override public Optional<InvitacionUsuario> findById(Long id) { return rows.stream().filter(r -> id.equals(r.getId())).findFirst(); }
        @Override public Optional<InvitacionUsuario> bloquearPorId(Long id) { return findById(id); }
        @Override public List<InvitacionUsuario> findByEmpresaIdOrderByIdDesc(Long empresaId) { return List.copyOf(rows); }
        @Override public long contarPendientes(Long empresaId, Instant ahora) { return rows.stream().filter(r -> r.disponible(ahora)).count(); }
        @Override public boolean existePendiente(Long empresaId, String email, Instant ahora) {
            return rows.stream().anyMatch(r -> r.disponible(ahora) && email.equals(r.getEmail()));
        }
    }
}
