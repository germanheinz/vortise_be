package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.MeDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.FotoPerfilStorage;
import com.vortise.gestion.application.port.FotoPerfilStorage.FotoPerfil;
import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilUseCase {
    static final int MAX_FOTO_BYTES = 2 * 1024 * 1024;

    private final UsuarioEmpresaRepository usuarios;
    private final CurrentTenantService tenant;
    private final SuscripcionConsultaService suscripciones;
    private final FotoPerfilStorage fotos;

    public PerfilUseCase(UsuarioEmpresaRepository usuarios, CurrentTenantService tenant, SuscripcionConsultaService suscripciones,
            FotoPerfilStorage fotos) {
        this.usuarios = usuarios; this.tenant = tenant; this.suscripciones = suscripciones; this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public MeDto me() {
        UsuarioEmpresa usuario = usuarioActual();
        Optional<Suscripcion> plan = suscripciones.planEfectivo(usuario);
        return new MeDto(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name(),
            usuario.getEmpresa().getId(), usuario.getEmpresa().getNombre(), usuario.isSuperAdmin(),
            plan.isPresent(), plan.map(s -> s.getPlan().getCodigo()).orElse(null), plan.map(Suscripcion::getTitular).orElse(null),
            fotos.existe(usuario.getId()));
    }

    public void subirFoto(byte[] datos) {
        if (datos == null || datos.length == 0) throw new SolicitudInvalidaException("La imagen está vacía");
        if (datos.length > MAX_FOTO_BYTES) throw new SolicitudInvalidaException("La imagen no puede superar 2 MB");
        TipoImagen tipo = TipoImagen.detectar(datos)
            .orElseThrow(() -> new SolicitudInvalidaException("Formato no soportado. Usá JPG, PNG o WEBP"));
        fotos.guardar(usuarioActual().getId(), datos, tipo);
    }

    public Optional<FotoPerfil> obtenerFoto() {
        return fotos.cargar(usuarioActual().getId());
    }

    public void eliminarFoto() {
        fotos.eliminar(usuarioActual().getId());
    }

    private UsuarioEmpresa usuarioActual() {
        return usuarios.findById(tenant.usuarioId())
            .filter(UsuarioEmpresa::isActivo)
            .orElseThrow(() -> new AccessDeniedException("Usuario no disponible"));
    }
}
