package com.vortise.gestion.service.authentication;

import com.vortise.gestion.service.EmpresaConfiguracionService;
import com.vortise.gestion.application.helper.PasswordPolicy;
import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
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
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String empresaNombre = request.empresaNombre().trim();
        if (usuarios.findByEmail(email).isPresent() || empresas.findByNombreIgnoreCase(empresaNombre).isPresent()) {
            throw new SolicitudInvalidaException(MENSAJE_DUPLICADO);
        }
        Empresa empresa = empresas.save(new Empresa(empresaNombre, null));
        UsuarioEmpresa usuario = usuarios.save(new UsuarioEmpresa(empresa, email, request.nombre().trim(),
            passwordEncoder.encode(request.password()), RolEmpresa.ADMINISTRADOR));
        empresa.setCreadoPorUsuarioId(usuario.getId());
        empresas.save(empresa);
        configuracion.inicializar(empresa.getId());
        return autenticacion.sesion(usuario);
    }
}
