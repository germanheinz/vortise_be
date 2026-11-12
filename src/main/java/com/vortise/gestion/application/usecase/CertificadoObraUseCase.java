package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CertificadoCrearDto;
import com.vortise.gestion.application.dto.CertificadoDetalleDto;
import com.vortise.gestion.application.dto.CertificadoEmpresaDto;
import com.vortise.gestion.application.dto.CertificadoExcedenteDto;
import com.vortise.gestion.application.dto.CertificadoFilaDto;
import com.vortise.gestion.application.dto.CertificadoResumenDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.application.usecase.CalculoCertificado.Avance;
import com.vortise.gestion.application.usecase.CalculoCertificado.Corte;
import com.vortise.gestion.application.usecase.CalculoCertificado.Fila;
import com.vortise.gestion.application.usecase.CalculoCertificado.Item;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CertificadoItemCompletado;
import com.vortise.gestion.domain.model.CertificadoObra;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.ContratoObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.CertificadoItemCompletadoRepository;
import com.vortise.gestion.domain.repository.CertificadoObraRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.domain.service.EstructuraRubros;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-company certificates of an obra, generated from its activities and progress loads. */
@Service
@Transactional
@SuppressWarnings("null")
public class CertificadoObraUseCase {
    private static final Locale ES = Locale.forLanguageTag("es-AR");

    private final ProyectoRepository proyectos;
    private final CertificadoObraRepository certificados;
    private final CertificadoItemCompletadoRepository completados;
    private final ObraTareaRepository tareas;
    private final CurrentIdentity identity;
    private final UsuarioEmpresaRepository usuarios;

    public CertificadoObraUseCase(ProyectoRepository proyectos, CertificadoObraRepository certificados,
            CertificadoItemCompletadoRepository completados, ObraTareaRepository tareas, CurrentIdentity identity,
            UsuarioEmpresaRepository usuarios) {
        this.proyectos = proyectos; this.certificados = certificados; this.completados = completados;
        this.tareas = tareas; this.identity = identity; this.usuarios = usuarios;
    }

    /** Activity of the obra that can be measured: a leaf of the structure; its root rubro is only the group title. */
    private record Actividad(RubroObra rubro, String codigo, String grupoCodigo, String grupoNombre, String nombre, Long empresa, ContratoObra contrato) { }

    /** Everything needed to measure the certificates of one company. */
    private record Contexto(Proyecto proyecto, Long empresa, List<Actividad> actividades, List<CertificadoObra> lista,
            List<List<Fila>> filas, List<ObraTarea> avances, Map<Long, CertificadoItemCompletado> marcas) { }

    @Transactional(readOnly = true)
    public List<CertificadoEmpresaDto> empresas(Long proyectoId) {
        Proyecto proyecto = proyecto(proyectoId);
        List<CertificadoObra> todos = certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyectoId);
        Map<Long, Integer> conteo = new LinkedHashMap<>();
        for (Actividad actividad : actividades(proyecto)) conteo.merge(actividad.empresa(), 1, Integer::sum);
        List<CertificadoEmpresaDto> result = new ArrayList<>();
        conteo.forEach((empresa, cantidad) -> {
            var propios = todos.stream().filter(item -> Objects.equals(item.getSubcontratistaId(), empresa)).toList();
            result.add(new CertificadoEmpresaDto(empresa, nombreEmpresa(proyecto, empresa), empresa == null, cantidad, propios.size(),
                propios.stream().anyMatch(item -> !item.isCerrado())));
        });
        result.sort(Comparator.comparing((CertificadoEmpresaDto item) -> !item.propia()).thenComparing(CertificadoEmpresaDto::nombre, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Transactional(readOnly = true)
    public List<CertificadoResumenDto> listar(Long proyectoId, Long empresa) {
        proyecto(proyectoId);
        var lista = deEmpresa(certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyectoId), empresa);
        List<CertificadoResumenDto> result = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) result.add(resumen(lista, i));
        return result;
    }

    public CertificadoDetalleDto crear(Long proyectoId, CertificadoCrearDto dto) {
        Proyecto proyecto = proyecto(proyectoId);
        Long empresa = dto.subcontratistaId();
        if (actividades(proyecto).stream().noneMatch(item -> Objects.equals(item.empresa(), empresa))) {
            throw new SolicitudInvalidaException("La empresa no tiene actividades asignadas en esta obra");
        }
        String periodicidad = dto.periodicidad().trim().toUpperCase(Locale.ROOT);
        if (!CertificadoObra.MENSUAL.equals(periodicidad) && !CertificadoObra.QUINCENAL.equals(periodicidad)) {
            throw new SolicitudInvalidaException("La periodicidad debe ser mensual o quincenal");
        }
        var existentes = deEmpresa(certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyectoId), empresa);
        if (existentes.stream().anyMatch(item -> !item.isCerrado())) {
            throw new SolicitudInvalidaException("Cerrá el certificado abierto de la empresa antes de crear el siguiente");
        }
        if (!existentes.isEmpty() && !dto.fechaCorte().isAfter(existentes.get(existentes.size() - 1).getFechaCorte())) {
            throw new SolicitudInvalidaException("La fecha de corte debe ser posterior a la del certificado anterior");
        }
        int numero;
        if (existentes.isEmpty()) {
            numero = CertificadoObra.MENSUAL.equals(periodicidad) && dto.numeroInicial() != null ? dto.numeroInicial() : 1;
            if (numero < 1 || numero > 9999) throw new SolicitudInvalidaException("El número inicial debe ser un entero positivo");
        } else {
            numero = existentes.stream().mapToInt(CertificadoObra::getNumero).max().orElse(0) + 1;
        }
        CertificadoObra certificado = new CertificadoObra();
        certificado.setProyecto(proyecto);
        certificado.setSubcontratista(empresa == null ? null : proyecto.getSubcontratistas().stream()
            .filter(item -> empresa.equals(item.getId())).findFirst().orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada")));
        certificado.setPeriodicidad(periodicidad);
        certificado.setNumero(numero);
        certificado.setFechaCorte(dto.fechaCorte());
        certificado.setEtiqueta(etiqueta(periodicidad, numero, dto.fechaCorte()));
        validarQuincena(existentes, certificado);
        certificado.setEstado(CertificadoObra.ABIERTO);
        certificado.setCreadoPorUsuarioId(identity.usuarioId());
        certificado.setCreadoEn(LocalDateTime.now());
        certificados.save(certificado);
        return detalleDe(proyecto, empresa, certificado.getId());
    }

    @Transactional(readOnly = true)
    public CertificadoDetalleDto detalle(Long proyectoId, Long certificadoId) {
        Proyecto proyecto = proyecto(proyectoId);
        return detalleDe(proyecto, certificado(proyectoId, certificadoId).getSubcontratistaId(), certificadoId);
    }

    public CertificadoDetalleDto editarCorte(Long proyectoId, Long certificadoId, LocalDate fechaCorte) {
        Proyecto proyecto = proyecto(proyectoId);
        CertificadoObra certificado = certificado(proyectoId, certificadoId);
        var lista = deEmpresa(certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyectoId), certificado.getSubcontratistaId());
        int index = indice(lista, certificadoId);
        if (index > 0 && !fechaCorte.isAfter(lista.get(index - 1).getFechaCorte())) {
            throw new SolicitudInvalidaException("La fecha de corte debe ser posterior a la del certificado anterior");
        }
        if (index < lista.size() - 1 && !fechaCorte.isBefore(lista.get(index + 1).getFechaCorte())) {
            throw new SolicitudInvalidaException("La fecha de corte debe ser anterior a la del certificado siguiente");
        }
        if (certificado.isCerrado() && fechaCorte.isAfter(LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha de corte de un certificado cerrado no puede ser futura");
        }
        String etiqueta = etiqueta(certificado.getPeriodicidad(), certificado.getNumero(), fechaCorte);
        var otros = lista.stream().filter(item -> !Objects.equals(item.getId(), certificadoId)).toList();
        CertificadoObra candidato = new CertificadoObra();
        candidato.setPeriodicidad(certificado.getPeriodicidad());
        candidato.setFechaCorte(fechaCorte);
        validarQuincena(otros, candidato);
        certificado.setFechaCorte(fechaCorte);
        certificado.setEtiqueta(etiqueta);
        certificados.save(certificado);
        return detalleDe(proyecto, certificado.getSubcontratistaId(), certificadoId);
    }

    public CertificadoDetalleDto cerrar(Long proyectoId, Long certificadoId) {
        Proyecto proyecto = proyecto(proyectoId);
        CertificadoObra certificado = certificado(proyectoId, certificadoId);
        if (certificado.isCerrado()) throw new SolicitudInvalidaException("El certificado ya está cerrado");
        if (certificado.getFechaCorte().isAfter(LocalDate.now())) {
            throw new SolicitudInvalidaException("No se puede cerrar un certificado con fecha de corte futura");
        }
        certificado.setEstado(CertificadoObra.CERRADO);
        certificado.setCerradoPorUsuarioId(identity.usuarioId());
        certificado.setCerradoEn(LocalDateTime.now());
        certificados.save(certificado);
        return detalleDe(proyecto, certificado.getSubcontratistaId(), certificadoId);
    }

    public CertificadoDetalleDto marcarCompletado(Long proyectoId, Long certificadoId, Long rubroId, boolean completado) {
        Proyecto proyecto = proyecto(proyectoId);
        CertificadoObra certificado = certificado(proyectoId, certificadoId);
        if (certificado.isCerrado()) throw new SolicitudInvalidaException("El certificado está cerrado");
        Contexto contexto = contexto(proyecto, certificado.getSubcontratistaId());
        int index = indice(contexto.lista(), certificadoId);
        int fila = indiceFila(contexto, rubroId);
        Fila medida = contexto.filas().get(index).get(fila);
        var marca = contexto.marcas().get(rubroId);
        if (completado) {
            if (marca != null) throw new SolicitudInvalidaException("El ítem ya está completado");
            Double total = contexto.actividades().get(fila).rubro().getCantidad();
            if (total == null || !(total > 0)) throw new SolicitudInvalidaException("El ítem no tiene cantidad contratada");
            if (medida.ejecutado() >= total) throw new SolicitudInvalidaException("El ítem ya llegó al 100%");
            CertificadoItemCompletado item = new CertificadoItemCompletado();
            item.setCertificado(certificado);
            item.setRubro(contexto.actividades().get(fila).rubro());
            item.setMarcadoPorUsuarioId(identity.usuarioId());
            item.setMarcadoEn(LocalDateTime.now());
            completados.save(item);
        } else if (marca != null) {
            if (!certificado.getId().equals(marca.getCertificado().getId())) {
                throw new SolicitudInvalidaException("El ítem se completó en un certificado anterior");
            }
            completados.delete(marca);
        }
        return detalleDe(proyecto, certificado.getSubcontratistaId(), certificadoId);
    }

    @Transactional(readOnly = true)
    public List<CertificadoExcedenteDto> excedentes(Long proyectoId) {
        Proyecto proyecto = proyecto(proyectoId);
        List<CertificadoObra> todos = certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyectoId);
        Set<Long> empresas = new java.util.LinkedHashSet<>();
        todos.forEach(item -> empresas.add(item.getSubcontratistaId()));
        Map<Long, String> nombres = new HashMap<>();
        List<CertificadoExcedenteDto> result = new ArrayList<>();
        for (Long empresa : empresas) {
            Contexto contexto = contexto(proyecto, empresa);
            for (int k = 0; k < contexto.lista().size(); k++) {
                CertificadoObra certificado = contexto.lista().get(k);
                LocalDate desde = k == 0 ? null : contexto.lista().get(k - 1).getFechaCorte();
                for (int i = 0; i < contexto.actividades().size(); i++) {
                    Fila fila = contexto.filas().get(k).get(i);
                    Actividad actividad = contexto.actividades().get(i);
                    Double total = actividad.rubro().getCantidad();
                    if (fila.excedente() > 0) {
                        ObraTarea carga = ultimaCarga(contexto.avances(), actividad.rubro().getId(), desde, certificado.getFechaCorte());
                        result.add(new CertificadoExcedenteDto(certificado.getId(), certificado.getEtiqueta(), nombreEmpresa(proyecto, empresa), "EXCEDENTE",
                            actividad.nombre(), actividad.rubro().getUnidad(), total, fila.ejecutado(), fila.excedente(),
                            CalculoCertificado.porcentaje(fila.excedente(), total), carga == null ? null : carga.getFecha(),
                            carga == null ? null : nombreUsuario(carga.getRegistradoPorUsuarioId(), nombres)));
                    }
                    if (fila.ajuste() > 0) {
                        var marca = contexto.marcas().get(actividad.rubro().getId());
                        result.add(new CertificadoExcedenteDto(certificado.getId(), certificado.getEtiqueta(), nombreEmpresa(proyecto, empresa), "AJUSTE",
                            actividad.nombre(), actividad.rubro().getUnidad(), total, fila.ejecutado(), fila.ajuste(),
                            CalculoCertificado.porcentaje(fila.ajuste(), total), marca == null ? null : marca.getMarcadoEn().toLocalDate(),
                            marca == null ? null : nombreUsuario(marca.getMarcadoPorUsuarioId(), nombres)));
                    }
                }
            }
        }
        return result;
    }

    private CertificadoDetalleDto detalleDe(Proyecto proyecto, Long empresa, Long certificadoId) {
        Contexto contexto = contexto(proyecto, empresa);
        int k = indice(contexto.lista(), certificadoId);
        CertificadoObra certificado = contexto.lista().get(k);
        List<CertificadoFilaDto> filas = new ArrayList<>();
        for (int i = 0; i < contexto.actividades().size(); i++) {
            Actividad actividad = contexto.actividades().get(i);
            Fila fila = contexto.filas().get(k).get(i);
            Double total = actividad.rubro().getCantidad();
            boolean puedeAlternar = !certificado.isCerrado() && (fila.marcadoEnEste()
                || (!fila.completado() && total != null && total > 0 && fila.ejecutado() < total));
            filas.add(new CertificadoFilaDto(actividad.rubro().getId(), actividad.codigo(), actividad.grupoCodigo(), actividad.grupoNombre(),
                actividad.nombre(), actividad.rubro().getUnidad(), total,
                fila.anterior(), CalculoCertificado.porcentaje(fila.anterior(), total),
                fila.actual(), CalculoCertificado.porcentaje(fila.actual(), total),
                fila.acumulado(), CalculoCertificado.porcentaje(fila.acumulado(), total),
                fila.excedente(), CalculoCertificado.porcentaje(fila.excedente(), total),
                fila.ajuste(), CalculoCertificado.porcentaje(fila.ajuste(), total),
                fila.completado(), puedeAlternar,
                actividad.contrato() == null ? null : actividad.contrato().getId(),
                actividad.contrato() == null ? null : actividad.contrato().getNombre()));
        }
        CertificadoResumenDto resumen = resumen(contexto.lista(), k);
        return new CertificadoDetalleDto(certificado.getId(), certificado.getNumero(), certificado.getEtiqueta(), certificado.getPeriodicidad(),
            resumen.periodoDesde(), certificado.getFechaCorte(), certificado.getEstado(), empresa, nombreEmpresa(proyecto, empresa), empresa == null,
            filas.stream().anyMatch(item -> item.excedenteCant() > 0), filas.stream().anyMatch(item -> item.ajusteCant() > 0), filas);
    }

    private Contexto contexto(Proyecto proyecto, Long empresa) {
        List<Actividad> actividades = actividades(proyecto).stream().filter(item -> Objects.equals(item.empresa(), empresa)).toList();
        List<CertificadoObra> lista = deEmpresa(certificados.findByProyectoIdOrderByFechaCorteAscIdAsc(proyecto.getId()), empresa);
        Map<Long, CertificadoItemCompletado> marcas = new HashMap<>();
        completados.findByCertificadoProyectoId(proyecto.getId()).forEach(item -> marcas.put(item.getRubroId(), item));
        Map<Long, Set<Long>> marcasPorCertificado = new HashMap<>();
        marcas.values().forEach(item -> marcasPorCertificado.computeIfAbsent(item.getCertificado().getId(), key -> new HashSet<>()).add(item.getRubroId()));
        List<ObraTarea> avances = tareas.findByProyectoId(proyecto.getId());
        List<Corte> cortes = lista.stream().map(item -> new Corte(item.getFechaCorte(), marcasPorCertificado.getOrDefault(item.getId(), Set.of()))).toList();
        List<Item> items = actividades.stream().map(item -> new Item(item.rubro().getId(), item.rubro().getCantidad())).toList();
        List<Avance> cantidades = avances.stream().filter(item -> item.getRubroId() != null && item.getCantidadReal() != null && item.getFecha() != null)
            .map(item -> new Avance(item.getRubroId(), item.getFecha(), item.getCantidadReal())).toList();
        return new Contexto(proyecto, empresa, actividades, lista, CalculoCertificado.calcular(items, cantidades, cortes), avances, marcas);
    }

    private static List<Actividad> actividades(Proyecto proyecto) {
        List<RubroObra> rubros = proyecto.getRubros() == null ? List.of() : proyecto.getRubros();
        List<Actividad> result = new ArrayList<>();
        for (RubroObra rubro : EstructuraRubros.hojas(rubros)) {
            RubroObra padre = EstructuraRubros.raiz(rubro, rubros);
            String nombre = rubro.getSubRubro() != null && !rubro.getSubRubro().isBlank() ? rubro.getSubRubro().trim() : String.valueOf(rubro.getRubro());
            result.add(new Actividad(rubro, rubro.getnRubro(), padre == null ? null : padre.getnRubro(),
                padre == null ? null : (padre.getRubro() == null || padre.getRubro().isBlank() ? "Rubro " + padre.getnRubro() : padre.getRubro().trim()),
                nombre, CertificadoBloqueos.empresaDe(rubro), padre == null ? null : padre.getContrato()));
        }
        return result;
    }

    private static List<CertificadoObra> deEmpresa(List<CertificadoObra> todos, Long empresa) {
        return todos.stream().filter(item -> Objects.equals(item.getSubcontratistaId(), empresa)).toList();
    }

    private static CertificadoResumenDto resumen(List<CertificadoObra> lista, int index) {
        CertificadoObra item = lista.get(index);
        return new CertificadoResumenDto(item.getId(), item.getNumero(), item.getEtiqueta(), item.getPeriodicidad(),
            index == 0 ? null : lista.get(index - 1).getFechaCorte().plusDays(1), item.getFechaCorte(), item.getEstado());
    }

    private static int indice(List<CertificadoObra> lista, Long certificadoId) {
        for (int i = 0; i < lista.size(); i++) {
            if (Objects.equals(lista.get(i).getId(), certificadoId)) return i;
        }
        throw new RecursoNoEncontradoException("Certificado no encontrado");
    }

    private static int indiceFila(Contexto contexto, Long rubroId) {
        for (int i = 0; i < contexto.actividades().size(); i++) {
            if (Objects.equals(contexto.actividades().get(i).rubro().getId(), rubroId)) return i;
        }
        throw new RecursoNoEncontradoException("La actividad no pertenece a esta empresa");
    }

    private static ObraTarea ultimaCarga(List<ObraTarea> avances, Long rubroId, LocalDate desde, LocalDate hasta) {
        return avances.stream().filter(item -> rubroId.equals(item.getRubroId()) && item.getFecha() != null && !item.getFecha().isAfter(hasta)
                && (desde == null || item.getFecha().isAfter(desde)))
            .max(Comparator.comparing(ObraTarea::getFecha).thenComparing(item -> item.getId() == null ? 0L : item.getId())).orElse(null);
    }

    private static String etiqueta(String periodicidad, int numero, LocalDate corte) {
        if (!CertificadoObra.QUINCENAL.equals(periodicidad)) return "Certificado N° " + numero;
        String mes = corte.getMonth().getDisplayName(TextStyle.FULL, ES);
        return "Quincena " + (corte.getDayOfMonth() <= 15 ? 1 : 2) + " – " + Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
    }

    /** A fortnight of a month can be certified once per company. */
    private static void validarQuincena(List<CertificadoObra> otros, CertificadoObra nuevo) {
        if (!CertificadoObra.QUINCENAL.equals(nuevo.getPeriodicidad())) return;
        for (CertificadoObra otro : otros) {
            if (!CertificadoObra.QUINCENAL.equals(otro.getPeriodicidad())) continue;
            if (otro.getFechaCorte().getYear() == nuevo.getFechaCorte().getYear() && otro.getFechaCorte().getMonth() == nuevo.getFechaCorte().getMonth()
                    && (otro.getFechaCorte().getDayOfMonth() <= 15) == (nuevo.getFechaCorte().getDayOfMonth() <= 15)) {
                throw new SolicitudInvalidaException("Ya existe un certificado de esa quincena para la empresa");
            }
        }
    }

    private String nombreEmpresa(Proyecto proyecto, Long empresa) {
        if (empresa == null) {
            String propia = proyecto.getEmpresaRelacionada() != null && proyecto.getEmpresaRelacionada().getNombre() != null
                ? proyecto.getEmpresaRelacionada().getNombre() : proyecto.getEmpresa();
            return (propia == null || propia.isBlank() ? "Mano de obra propia" : propia.trim()) + " (propia)";
        }
        return proyecto.getSubcontratistas().stream().filter(item -> empresa.equals(item.getId())).map(CatalogoEmpresa::getNombre)
            .findFirst().orElse("Empresa " + empresa);
    }

    private String nombreUsuario(Long usuarioId, Map<Long, String> cache) {
        if (usuarioId == null) return null;
        String nombre = cache.computeIfAbsent(usuarioId, id -> usuarios.findById(id).map(item -> item.getNombre()).orElse(""));
        return nombre.isEmpty() ? null : nombre;
    }

    private Proyecto proyecto(Long proyectoId) {
        return proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
    }

    private CertificadoObra certificado(Long proyectoId, Long certificadoId) {
        return certificados.findByIdAndProyectoId(certificadoId, proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Certificado no encontrado"));
    }
}
