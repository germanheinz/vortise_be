package com.vortise.gestion.controller.authentication;

import com.vortise.gestion.application.dto.InvitacionAceptarDto;
import com.vortise.gestion.application.dto.InvitacionValidaDto;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.service.usuario.ClerkUserSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitacion")
public class InvitacionController {
    private final UsuarioEmpresaRepository usuarioRepository;
    private final ClerkUserSyncService clerkSync;

    @Autowired
    public InvitacionController(UsuarioEmpresaRepository usuarioRepository, ClerkUserSyncService clerkSync) {
        this.usuarioRepository = usuarioRepository;
        this.clerkSync = clerkSync;
    }

    @GetMapping("/validar")
    public ResponseEntity<InvitacionValidaDto> validarCodigo(@RequestParam String codigo) {
        var usuario = usuarioRepository.findByCodigoInvitacion(codigo);
        
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new InvitacionValidaDto(null, null, null, false));
        }
        
        UsuarioEmpresa u = usuario.get();
        
        if (!u.esInvitacionValida()) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body(new InvitacionValidaDto(u.getEmail(), u.getNombre(), u.getEmpresa().getNombre(), false));
        }
        
        return ResponseEntity.ok(new InvitacionValidaDto(
            u.getEmail(),
            u.getNombre(),
            u.getEmpresa().getNombre(),
            true
        ));
    }

    @PostMapping("/aceptar")
    public ResponseEntity<InvitacionValidaDto> aceptarInvitacion(@RequestBody InvitacionAceptarDto dto) {
        var usuario = usuarioRepository.findByCodigoInvitacion(dto.codigo());
        
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new InvitacionValidaDto(null, null, null, false));
        }
        
        UsuarioEmpresa u = usuario.get();
        
        if (!u.esInvitacionValida()) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body(new InvitacionValidaDto(u.getEmail(), u.getNombre(), u.getEmpresa().getNombre(), false));
        }
        
        // Validar que email coincide
        if (!u.getEmail().equalsIgnoreCase(dto.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new InvitacionValidaDto(u.getEmail(), u.getNombre(), u.getEmpresa().getNombre(), false));
        }
        
        // Aceptar invitación
        u = clerkSync.aceptarInvitacion(u, dto.clerkUserId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new InvitacionValidaDto(u.getEmail(), u.getNombre(), u.getEmpresa().getNombre(), true));
    }
}
