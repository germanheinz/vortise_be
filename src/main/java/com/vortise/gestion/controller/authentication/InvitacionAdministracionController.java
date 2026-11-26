package com.vortise.gestion.controller.authentication;

import com.vortise.gestion.application.dto.InvitacionUsuarioDto;
import com.vortise.gestion.application.dto.InvitacionesDto;
import com.vortise.gestion.infrastructure.security.Guards;
import com.vortise.gestion.service.invitacion.InvitacionUsuarioService;
import com.vortise.gestion.service.invitacion.InvitacionesService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvitacionAdministracionController {
    private static final String INVITACIONES_EQUIPO = "/api/equipo/invitaciones";
    private static final String INVITACIONES_USUARIO = "/api/empresas/{empresaId}/usuarios/invitaciones";

    private final InvitacionesService equipo;
    private final InvitacionUsuarioService usuario;

    public InvitacionAdministracionController(InvitacionesService equipo, InvitacionUsuarioService usuario) {
        this.equipo = equipo;
        this.usuario = usuario;
    }

    @GetMapping(INVITACIONES_EQUIPO)
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public InvitacionesDto.Equipo listarEquipo() {
        return equipo.listar();
    }

    @PostMapping(INVITACIONES_EQUIPO)
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<InvitacionesDto.Creada> crearEquipo(@Valid @RequestBody InvitacionesDto.Crear dto) {
        return ResponseEntity.status(201).body(equipo.crear(dto));
    }

    @DeleteMapping(INVITACIONES_EQUIPO + "/{id}")
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<Void> revocarEquipo(@PathVariable Long id) {
        equipo.revocar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(INVITACIONES_USUARIO)
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public List<InvitacionUsuarioDto.Resumen> listarUsuario(@PathVariable Long empresaId) {
        return usuario.listar(empresaId);
    }

    @PostMapping(INVITACIONES_USUARIO)
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<InvitacionUsuarioDto.Creada> crearUsuario(
            @PathVariable Long empresaId, @Valid @RequestBody InvitacionUsuarioDto.Crear dto) {
        return ResponseEntity.status(201).header("Cache-Control", "no-store").body(usuario.crear(empresaId, dto));
    }

    @DeleteMapping(INVITACIONES_USUARIO + "/{id}")
    @PreAuthorize(Guards.SOLO_ADMINISTRADOR)
    public ResponseEntity<Void> revocarUsuario(@PathVariable Long empresaId, @PathVariable Long id) {
        usuario.revocar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
