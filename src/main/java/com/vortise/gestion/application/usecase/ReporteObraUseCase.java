package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ReporteBorradorImagenesDto;
import com.vortise.gestion.application.dto.ReporteDetalleDto;
import com.vortise.gestion.application.dto.ReporteEmitirDto;
import com.vortise.gestion.application.dto.ReporteImagenDto;
import com.vortise.gestion.application.dto.ReporteResumenDto;
import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;
import com.vortise.gestion.application.port.ImagenReporteStorage;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.ReporteEmitido;
import com.vortise.gestion.domain.model.ReporteImagen;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.ReporteEmitidoRepository;
import com.vortise.gestion.domain.repository.ReporteImagenRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Report images (hosted outside the database) and the history of issued reports of an obra. */
@Service
@Transactional
public class ReporteObraUseCase {
    public static final int MAX_IMAGENES = 6;
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_SNAPSHOT_CHARS = 1_000_000;
    static final int MAX_DIAS_SEMANA = 31;
    static final int VERSION_ESQUEMA = 1;
    private static final Logger log = LoggerFactory.getLogger(ReporteObraUseCase.class);

    private final ProyectoRepository proyectos;
    private final ReporteEmitidoRepository reportes;
    private final ReporteImagenRepository imagenes;
    private final ImagenReporteStorage storage;
    private final CurrentIdentity identity;
    private final UsuarioEmpresaRepository usuarios;
    private final ObjectMapper mapper;

    public ReporteObraUseCase(ProyectoRepository proyectos, ReporteEmitidoRepository reportes, ReporteImagenRepository imagenes,
            ImagenReporteStorage storage, CurrentIdentity identity, UsuarioEmpresaRepository usuarios, ObjectMapper mapper) {
        this.proyectos = proyectos; this.reportes = reportes; this.imagenes = imagenes; this.storage = storage;
        this.identity = identity; this.usuarios = usuarios; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ReporteBorradorImagenesDto borrador(Long proyectoId) {
        proyecto(proyectoId);
        return new ReporteBorradorImagenesDto(storage.disponible(), MAX_IMAGENES, MAX_BYTES,
            imagenes.findByProyectoIdAndReporteIsNullOrderByOrdenAsc(proyectoId).stream().map(ReporteObraUseCase::toDto).toList());
    }

    public ReporteImagenDto subirImagen(Long proyectoId, byte[] datos, String leyenda) {
        Proyecto proyecto = proyecto(proyectoId);
        if (!storage.disponible()) throw new FeatureUnavailableException("La subida de imágenes no está configurada.");
        if (datos == null || datos.length == 0) throw new SolicitudInvalidaException("La imagen está vacía");
        if (datos.length > MAX_BYTES) throw new SolicitudInvalidaException("Cada imagen puede pesar hasta 5 MB");
        TipoImagen tipo = TipoImagen.detectar(datos)
            .orElseThrow(() -> new SolicitudInvalidaException("Formato no soportado. Usá JPG, PNG o WEBP"));
        String leyendaLimpia = limpiar(leyenda);
        List<ReporteImagen> borrador = imagenes.findByProyectoIdAndReporteIsNullOrderByOrdenAsc(proyectoId);
        if (borrador.size() >= MAX_IMAGENES) throw new SolicitudInvalidaException("El reporte admite hasta " + MAX_IMAGENES + " imágenes");

        var subida = storage.subir(proyectoId, datos, tipo);
        ReporteImagen imagen = new ReporteImagen();
        imagen.setProyecto(proyecto);
        imagen.setPublicId(subida.publicId());
        imagen.setUrl(subida.url());
        imagen.setAncho(subida.ancho());
        imagen.setAlto(subida.alto());
        imagen.setTamanoBytes(subida.tamanoBytes());
        imagen.setLeyenda(leyendaLimpia);
        imagen.setOrden(borrador.stream().map(ReporteImagen::getOrden).max(Integer::compare).orElse(0) + 1);
        imagen.setSubidoPorUsuarioId(identity.usuarioId());
        imagen.setSubidoEn(LocalDateTime.now());
        try {
            return toDto(imagenes.save(imagen));
        } catch (RuntimeException ex) {
            quitarDelAlmacen(subida.publicId());
            throw ex;
        }
    }

    public ReporteImagenDto actualizarLeyenda(Long proyectoId, Long imagenId, String leyenda) {
        ReporteImagen imagen = borradorDe(proyectoId, imagenId);
        imagen.setLeyenda(limpiar(leyenda));
        return toDto(imagenes.save(imagen));
    }

    public void eliminarImagen(Long proyectoId, Long imagenId) {
        ReporteImagen imagen = borradorDe(proyectoId, imagenId);
        String publicId = imagen.getPublicId();
        imagenes.delete(imagen);
        quitarDelAlmacen(publicId);
    }

    public ReporteDetalleDto emitir(Long proyectoId, ReporteEmitirDto dto) {
        Proyecto proyecto = proyecto(proyectoId);
        if (dto.semanaHasta().isBefore(dto.semanaDesde())
            || ChronoUnit.DAYS.between(dto.semanaDesde(), dto.semanaHasta()) >= MAX_DIAS_SEMANA) {
            throw new SolicitudInvalidaException("El período del informe no es válido");
        }
        JsonNode snapshot = dto.snapshot();
        if (snapshot == null || !snapshot.isObject()) throw new SolicitudInvalidaException("El contenido del reporte no es válido");
        String json = mapper.writeValueAsString(snapshot);
        if (json.length() > MAX_SNAPSHOT_CHARS) throw new SolicitudInvalidaException("El reporte es demasiado grande");

        ReporteEmitido reporte = new ReporteEmitido();
        reporte.setProyecto(proyecto);
        reporte.setNumero(reportes.ultimoNumero(proyectoId) + 1);
        reporte.setSemanaDesde(dto.semanaDesde());
        reporte.setSemanaHasta(dto.semanaHasta());
        reporte.setFechaCorte(dto.fechaCorte());
        reporte.setEmitidoPorUsuarioId(identity.usuarioId());
        reporte.setEmitidoPor(usuarios.findById(identity.usuarioId()).map(UsuarioEmpresa::getNombre).orElse(null));
        reporte.setEmitidoEn(LocalDateTime.now());
        reporte.setVersionEsquema(VERSION_ESQUEMA);
        reporte.setSnapshot(json);
        ReporteEmitido guardado = reportes.save(reporte);

        List<ReporteImagen> adjuntas = imagenes.findByProyectoIdAndReporteIsNullOrderByOrdenAsc(proyectoId);
        adjuntas.forEach(imagen -> { imagen.setReporte(guardado); imagenes.save(imagen); });
        return detalle(guardado, adjuntas);
    }

    @Transactional(readOnly = true)
    public List<ReporteResumenDto> listar(Long proyectoId) {
        proyecto(proyectoId);
        List<ReporteEmitido> lista = reportes.findByProyectoIdOrderByNumeroDesc(proyectoId);
        Collection<Long> ids = lista.stream().map(ReporteEmitido::getId).toList();
        Map<Long, Long> cantidades = ids.isEmpty() ? Map.of() : imagenes.findByReporteIdIn(ids).stream()
            .collect(Collectors.groupingBy(imagen -> imagen.getReporte().getId(), Collectors.counting()));
        return lista.stream().map(reporte -> new ReporteResumenDto(reporte.getId(), reporte.getNumero(), reporte.getSemanaDesde(),
            reporte.getSemanaHasta(), reporte.getFechaCorte(), reporte.getEmitidoPor(), reporte.getEmitidoEn(),
            cantidades.getOrDefault(reporte.getId(), 0L).intValue())).toList();
    }

    @Transactional(readOnly = true)
    public ReporteDetalleDto detalle(Long proyectoId, Long reporteId) {
        proyecto(proyectoId);
        ReporteEmitido reporte = reportes.findByIdAndProyectoId(reporteId, proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado"));
        return detalle(reporte, imagenes.findByReporteIdOrderByOrdenAsc(reporteId));
    }

    private ReporteDetalleDto detalle(ReporteEmitido reporte, List<ReporteImagen> adjuntas) {
        return new ReporteDetalleDto(reporte.getId(), reporte.getNumero(), reporte.getSemanaDesde(), reporte.getSemanaHasta(),
            reporte.getFechaCorte(), reporte.getEmitidoPor(), reporte.getEmitidoEn(), reporte.getVersionEsquema(),
            mapper.readTree(reporte.getSnapshot()), adjuntas.stream().map(ReporteObraUseCase::toDto).toList());
    }

    private Proyecto proyecto(Long proyectoId) {
        return proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
    }

    private ReporteImagen borradorDe(Long proyectoId, Long imagenId) {
        proyecto(proyectoId);
        ReporteImagen imagen = imagenes.findByIdAndProyectoId(imagenId, proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Imagen no encontrada"));
        if (imagen.getReporte() != null) throw new SolicitudInvalidaException("La imagen pertenece a un reporte ya emitido");
        return imagen;
    }

    /** Orphans in the storage are cheaper than a dangling database row, so a failed removal is only logged. */
    private void quitarDelAlmacen(String publicId) {
        try {
            storage.eliminar(publicId);
        } catch (RuntimeException ex) {
            log.warn("Could not remove report image {} from storage: {}", publicId, ex.getClass().getSimpleName());
        }
    }

    private static String limpiar(String leyenda) {
        if (leyenda == null) return null;
        String limpia = leyenda.replaceAll("\\p{Cntrl}", " ").trim();
        if (limpia.length() > 255) throw new SolicitudInvalidaException("La leyenda no puede superar los 255 caracteres");
        return limpia.isEmpty() ? null : limpia;
    }

    private static ReporteImagenDto toDto(ReporteImagen imagen) {
        return new ReporteImagenDto(imagen.getId(), imagen.getUrl(), imagen.getLeyenda(), imagen.getAncho(), imagen.getAlto(), imagen.getOrden());
    }
}
