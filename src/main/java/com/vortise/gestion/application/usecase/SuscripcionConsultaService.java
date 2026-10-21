package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.SuscripcionResumenDto;
import com.vortise.gestion.domain.model.PagoSuscripcion;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SuscripcionConsultaService {
    private final SuscripcionRepository suscripciones;
    private final PagoSuscripcionRepository pagos;

    public SuscripcionConsultaService(SuscripcionRepository suscripciones, PagoSuscripcionRepository pagos) {
        this.suscripciones = suscripciones;
        this.pagos = pagos;
    }

    /** Best active subscription covering the user: their own or their company's, highest plan level wins. */
    public Optional<Suscripcion> planEfectivo(UsuarioEmpresa usuario) {
        Instant now = Instant.now();
        return Stream.concat(suscripciones.findByUsuarioId(usuario.getId()).stream(),
                suscripciones.findByEmpresaId(usuario.getEmpresa().getId()).stream())
            .filter(s -> s.getPlan() != null && s.isActiva(now))
            .max(Comparator.comparingInt(s -> s.getPlan().getNivel()));
    }

    /** Admin view: effective subscription, otherwise the user's own (possibly inactive) one. */
    public SuscripcionResumenDto resumenUsuario(UsuarioEmpresa usuario) {
        return planEfectivo(usuario).or(() -> suscripciones.findByUsuarioId(usuario.getId()))
            .map(this::resumen).orElse(null);
    }

    public Optional<SuscripcionResumenDto> resumenPropio(Long usuarioId) {
        return suscripciones.findByUsuarioId(usuarioId).map(this::resumen);
    }

    public Optional<SuscripcionResumenDto> resumenEmpresa(Long empresaId) {
        return suscripciones.findByEmpresaId(empresaId).map(this::resumen);
    }

    SuscripcionResumenDto resumen(Suscripcion s) {
        Optional<PagoSuscripcion> ultimo = s.getId() == null ? Optional.empty() : pagos.findFirstBySuscripcionIdOrderByFechaDesc(s.getId());
        return new SuscripcionResumenDto(
            s.getTitular(),
            s.getPlan() == null ? null : s.getPlan().getCodigo(),
            s.getPlan() == null ? null : s.getPlan().getNombre(),
            s.getEstado(),
            s.isActiva(Instant.now()),
            s.getFinPrueba(),
            s.getFinPeriodoActual(),
            s.isCancelaAlFinal(),
            ultimo.map(PagoSuscripcion::getFecha).orElse(null),
            ultimo.map(p -> BigDecimal.valueOf(p.getMontoCentavos(), 2)).orElse(null),
            ultimo.map(PagoSuscripcion::getMoneda).orElse(null),
            ultimo.map(PagoSuscripcion::getEstado).orElse(null));
    }
}
