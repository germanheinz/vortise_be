package com.vortise.gestion.service.authentication;

import com.vortise.gestion.application.helper.Emails;
import com.vortise.gestion.service.empresa.EmpresaConfiguracionService;
import com.vortise.gestion.application.helper.PasswordPolicy;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroConIdentidadDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.IdentidadExternaVerificador.IdentidadVerificada;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.UsuarioEmpresaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {
    // Same message for every duplicate, so the endpoint does not reveal which emails exist.
    static final String MENSAJE_DUPLICADO = "No se pudo completar el registro. Si ya tenés una cuenta, iniciá sesión.";

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacionService autenticacion;

    private final EmpresaConfiguracionService configuracion;

    public RegistroService(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios, PasswordEncoder passwordEncoder,
            AutenticacionService autenticacion, EmpresaConfiguracionService configuracion) {
        this.empresas = empresas; this.usuarios = usuarios; this.passwordEncoder = passwordEncoder; this.autenticacion = autenticacion; this.configuracion = configuracion;
    }

    @Transactional
    public LoginResponseDto registrar(RegistroRequestDto request) {
        PasswordPolicy.validate(request.password());
        String password = passwordEncoder.encode(request.password());
        return crearEmpresaConAdministrador(request.empresaNombre(), request.email(),
            empresa -> new UsuarioEmpresa(empresa, Emails.normalizar(request.email()), request.nombre().trim(), password, RolEmpresa.ADMINISTRADOR));
    }

    @Transactional
    public LoginResponseDto registrarConIdentidad(RegistroConIdentidadDto request, IdentidadVerificada identidad) {
        if (usuarios.findByClerkUserId(identidad.sujeto()).isPresent()) throw new SolicitudInvalidaException(MENSAJE_DUPLICADO);
        return crearEmpresaConAdministrador(request.empresaNombre(), identidad.email(),
            empresa -> UsuarioEmpresa.conIdentidadExterna(empresa, Emails.normalizar(identidad.email()), request.nombre().trim(),
                RolEmpresa.ADMINISTRADOR, identidad.sujeto()));
    }

    private LoginResponseDto crearEmpresaConAdministrador(String nombreEmpresa, String email,
            java.util.function.Function<Empresa, UsuarioEmpresa> administrador) {
        String empresaNombre = nombreEmpresa.trim();
        if (usuarios.findByEmail(Emails.normalizar(email)).isPresent() || empresas.findByNombreIgnoreCase(empresaNombre).isPresent()) {
            throw new SolicitudInvalidaException(MENSAJE_DUPLICADO);
        }
        Empresa empresa = empresas.save(new Empresa(empresaNombre, null));
        UsuarioEmpresa usuario = usuarios.save(administrador.apply(empresa));
        empresa.setCreadoPorUsuarioId(usuario.getId());
        empresas.save(empresa);
        configuracion.inicializar(empresa.getId());
        return autenticacion.sesion(usuario);
    }
}
