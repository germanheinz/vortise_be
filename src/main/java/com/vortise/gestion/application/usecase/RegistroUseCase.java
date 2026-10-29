package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.dto.RegistroRequestDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroUseCase {
    // Same message for every duplicate, so the endpoint does not reveal which emails exist.
    static final String MENSAJE_DUPLICADO = "No se pudo completar el registro. Si ya tenés una cuenta, iniciá sesión.";

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacionUseCase autenticacion;

    private final InicializarEmpresaUseCase inicializar;

    public RegistroUseCase(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios, PasswordEncoder passwordEncoder,
            AutenticacionUseCase autenticacion, InicializarEmpresaUseCase inicializar) {
        this.empresas = empresas; this.usuarios = usuarios; this.passwordEncoder = passwordEncoder; this.autenticacion = autenticacion; this.inicializar = inicializar;
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
        inicializar.execute(empresa.getId());
        return autenticacion.sesion(usuario);
    }
}
