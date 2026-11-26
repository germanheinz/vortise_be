package com.vortise.gestion.service;

import com.vortise.gestion.service.equipo.EquipoAccessPolicy;
import com.vortise.gestion.service.invitacion.InvitacionesService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.InvitacionesDto.AceptarConIdentidad;
import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.helper.InvitacionToken;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.application.port.IdentidadExternaVerificador.IdentidadVerificada;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.InvitacionEquipo;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.Suscripcion;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.InvitacionEquipoRepository;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.service.authentication.AutenticacionService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class InvitacionesServiceIdentidadTest {
    private static final String TOKEN = InvitacionToken.nuevo();
    private static final long TITULAR = 9L;
    private static final long EMPRESA = 1L;
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final InvitacionEquipoRepository invitaciones = mock(InvitacionEquipoRepository.class);
    private final EquipoAccessPolicy policy = mock(EquipoAccessPolicy.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final InvitacionesService useCase = new InvitacionesService(usuarios, empresas, invitaciones, mock(CurrentIdentity.class),
        policy, encoder, mock(AutenticacionService.class), "https://app.example.invalid");
    private InvitacionEquipo invitacion;

    @BeforeEach
    void setUp() {
        invitacion = new InvitacionEquipo(InvitacionToken.hash(TOKEN), TITULAR, Set.of(EMPRESA), Instant.now());
        ReflectionTestUtils.setField(invitacion, "id", 5L);
        var identidad = mock(InvitacionEquipoRepository.Identidad.class);
        when(identidad.getId()).thenReturn(5L);
        when(identidad.getTitularId()).thenReturn(TITULAR);
        when(invitaciones.buscarIdentidad(InvitacionToken.hash(TOKEN))).thenReturn(Optional.of(identidad));
        when(invitaciones.bloquearPorId(5L)).thenReturn(Optional.of(invitacion));
        when(usuarios.bloquearPorId(TITULAR)).thenReturn(Optional.of(mock(UsuarioEmpresa.class)));
        when(policy.coberturasPro(TITULAR, Set.of(EMPRESA))).thenReturn(Map.of(EMPRESA, mock(Suscripcion.class)));
        Empresa empresa = new Empresa("Constructora", null);
        ReflectionTestUtils.setField(empresa, "id", EMPRESA);
        when(empresas.findById(EMPRESA)).thenReturn(Optional.of(empresa));
        when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aVerifiedIdentityJoinsAsCollaboratorWithItsOwnEmailAndNoLocalPassword() {
        useCase.aceptarConIdentidad(new AceptarConIdentidad(TOKEN, " Ana "), new IdentidadVerificada("user_ana", " Ana@Example.invalid "));

        var saved = ArgumentCaptor.forClass(UsuarioEmpresa.class);
        verify(usuarios).save(saved.capture());
        UsuarioEmpresa user = saved.getValue();
        assertEquals("ana@example.invalid", user.getEmail());
        assertEquals("Ana", user.getNombre());
        assertEquals(RolEmpresa.COLABORADOR, user.getRol());
        assertEquals("user_ana", user.getClerkUserId());
        assertEquals(UsuarioEmpresa.SIN_CONTRASENA_LOCAL, user.getPasswordHash());
        assertEquals(Set.of(EMPRESA), user.getEmpresasAsignadas());
        assertFalse(invitacion.disponible(Instant.now()));
        verifyNoInteractions(encoder);
    }

    @Test
    void anIdentityAlreadyLinkedCannotAcceptAndTheInvitationStaysUsable() {
        when(usuarios.findByClerkUserId("user_linked")).thenReturn(Optional.of(mock(UsuarioEmpresa.class)));

        assertThrows(ConflictoEstadoException.class, () -> useCase.aceptarConIdentidad(new AceptarConIdentidad(TOKEN, "Ana"),
            new IdentidadVerificada("user_linked", "ana@example.invalid")));

        verify(usuarios, never()).save(any());
        assertTrue(invitacion.disponible(Instant.now()));
    }
}
