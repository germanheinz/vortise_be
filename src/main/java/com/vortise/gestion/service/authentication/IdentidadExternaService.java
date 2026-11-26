package com.vortise.gestion.service.authentication;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.exception.AccesoDenegadoException;
import com.vortise.gestion.application.port.IdentidadExternaVerificador.IdentidadVerificada;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens a session for a verified external identity. Accounts are never created here: access comes from registering
 * a company or accepting an invitation. An existing user is linked on first sign-in when the provider reports the
 * same email as verified and the user is not linked to another external account yet.
 */
@Service
public class IdentidadExternaService {
    private static final Logger log = LoggerFactory.getLogger(IdentidadExternaService.class);

    private final UsuarioEmpresaRepository usuarios;
    private final AutenticacionService autenticacion;

    public IdentidadExternaService(UsuarioEmpresaRepository usuarios, AutenticacionService autenticacion) {
        this.usuarios = usuarios;
        this.autenticacion = autenticacion;
    }

    @Transactional
    public LoginResponseDto iniciarSesion(IdentidadVerificada identidad) {
        UsuarioEmpresa usuario = usuarios.findByClerkUserId(identidad.sujeto())
            .orElseGet(() -> vincularPorEmail(identidad));
        if (!usuario.isActivo()) throw AccesoDenegadoException.cuentaInactiva();
        usuario.setUltimoLoginExitosoEn(Instant.now());
        usuarios.save(usuario);
        return autenticacion.sesion(usuario);
    }

    private UsuarioEmpresa vincularPorEmail(IdentidadVerificada identidad) {
        String email = identidad.email().trim().toLowerCase(Locale.ROOT);
        UsuarioEmpresa usuario = usuarios.findByEmail(email)
            .filter(candidato -> candidato.getClerkUserId() == null)
            .orElseThrow(AccesoDenegadoException::cuentaNoRegistrada);
        usuario.vincularIdentidadExterna(identidad.sujeto());
        log.info("User {} linked to an external identity", usuario.getId());
        return usuario;
    }
}
