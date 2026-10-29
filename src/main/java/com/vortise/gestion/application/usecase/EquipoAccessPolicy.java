package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Live team entitlement; neither JWT claims nor browser role hints grant membership. */
@Service
@Transactional(readOnly = true)
public class EquipoAccessPolicy {
    public static final int MAX_INVITADOS = 5;
    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final SuscripcionRepository suscripciones;
    public EquipoAccessPolicy(UsuarioEmpresaRepository usuarios, EmpresaRepository empresas, SuscripcionRepository suscripciones) {
        this.usuarios = usuarios; this.empresas = empresas; this.suscripciones = suscripciones;
    }
    public Optional<Suscripcion> coberturaPro(Long titularId, Long empresaId) {
        return Optional.ofNullable(coberturasPro(titularId, Set.of(empresaId)).get(empresaId));
    }
    /** Constant number of repository reads, even for multi-company invitations. */
    public Map<Long, Suscripcion> coberturasPro(Long titularId, Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var titular = usuarios.findById(titularId).filter(u -> u.isActivo() && u.getRol() == RolEmpresa.ADMINISTRADOR);
        if (titular.isEmpty()) return Map.of();
        var propias = empresas.findByIdIn(ids).stream().filter(e -> titularId.equals(e.getCreadoPorUsuarioId())).toList();
        if (propias.isEmpty()) return Map.of();
        Instant now = Instant.now();
        var personal = suscripciones.findByUsuarioId(titularId).filter(s -> proActivo(s, now));
        Map<Long, Suscripcion> result = new HashMap<>();
        if (personal.isPresent()) {
            for (Empresa empresa : propias) result.put(empresa.getId(), personal.get());
        } else {
            var propiasIds = propias.stream().map(Empresa::getId).toList();
            for (Suscripcion subscription : suscripciones.findByEmpresaIdIn(propiasIds)) {
                if (proActivo(subscription, now)) result.put(subscription.getEmpresa().getId(), subscription);
            }
        }
        return Map.copyOf(result);
    }
    private boolean proActivo(Suscripcion subscription, Instant now) {
        return subscription.isActiva(now) && subscription.getPlan() != null && "PRO".equals(subscription.getPlan().getCodigo());
    }
    public Set<Long> empresasPermitidas(UsuarioEmpresa usuario) {
        if (!usuario.isColaborador() || !usuario.isActivo() || usuario.getInvitanteId() == null) return Set.of();
        UsuarioEmpresa actual = usuarios.findById(usuario.getId()).orElse(null);
        if (actual == null || !actual.isActivo() || !actual.isColaborador() || actual.getInvitanteId() == null) return Set.of();
        return coberturasPro(actual.getInvitanteId(), actual.getEmpresasAsignadas()).keySet();
    }
    public boolean puedeAcceder(UsuarioEmpresa usuario, Long empresaId) {
        return empresasPermitidas(usuario).contains(empresaId);
    }
    public Empresa empresaInicial(UsuarioEmpresa usuario) {
        if (!usuario.isColaborador()) return usuario.getEmpresa();
        return empresasPermitidas(usuario).stream().sorted().flatMap(id -> empresas.findById(id).stream()).findFirst()
            .orElseThrow(() -> new AccessDeniedException("El equipo no tiene cobertura Pro activa."));
    }
    public void requireAcceso(UsuarioEmpresa usuario) {
        if (usuario.isColaborador() && empresasPermitidas(usuario).isEmpty()) {
            throw new AccessDeniedException("El acceso del equipo está suspendido hasta que el administrador tenga Pro activo.");
        }
    }
}
