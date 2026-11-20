package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.ConsultarRestriccionesObraUseCase;
import com.vortise.gestion.application.usecase.ReporteObraUseCase;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
class TelegramConversacion {
    private static final String CONSULTAR = "Consultar restricciones";
    private static final String CREAR = "Crear restricción en obra";
    private static final String ESTADO = "Consultar estado de obra";
    private static final String MENU_PRINCIPAL = "Volver al menú";
    private static final String SIGUIENTE = "Más obras";
    private static final String ANTERIOR = "Obras anteriores";
    static final List<String> MENU = List.of(CONSULTAR, CREAR, ESTADO);
    private static final java.util.regex.Pattern BOTON = java.util.regex.Pattern.compile("op:(\\d{1,12}):(\\d{1,2})");
    private static final int POR_PAGINA = 6;
    private static final ZoneId ZONA_OBRA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d/M/uuuu")
        .withResolverStyle(ResolverStyle.STRICT);

    private final ConcurrentHashMap<Long, Sesion> sesiones = new ConcurrentHashMap<>();
    private final ProyectoRepository proyectos;
    private final EmpresaRepository empresas;
    private final CurrentTenantService identidad;
    private final ReporteObraUseCase reportes;
    private final ConsultarRestriccionesObraUseCase restricciones;

    TelegramConversacion(ProyectoRepository proyectos, EmpresaRepository empresas, CurrentTenantService identidad,
            ReporteObraUseCase reportes, ConsultarRestriccionesObraUseCase restricciones) {
        this.proyectos = proyectos;
        this.empresas = empresas;
        this.identidad = identidad;
        this.reportes = reportes;
        this.restricciones = restricciones;
    }

    TelegramController.Respuesta responder(long chat, long usuario, String nombre, long evento, String texto, UnaryOperator<String> ejecutar) {
        Instant ahora = Instant.now();
        sesiones.entrySet().removeIf(entry -> entry.getValue().vence().isBefore(ahora));
        return sesiones.compute(chat, (id, anterior) -> {
            if (anterior != null && anterior.usuario() == usuario && evento <= anterior.evento()) return anterior;
            Paso paso = anterior != null && anterior.usuario() == usuario ? anterior.paso() : null;
            String entrada = texto;
            var boton = BOTON.matcher(texto);
            if (boton.matches()) {
                int indice = Integer.parseInt(boton.group(2));
                if (anterior != null && anterior.usuario() == usuario) {
                    if (Long.parseLong(boton.group(1)) != anterior.evento() || indice >= anterior.respuesta().opciones().size()) {
                        return anterior;
                    }
                    entrada = anterior.respuesta().opciones().get(indice);
                } else {
                    entrada = "/menu";
                }
            }
            if ((paso == null || paso.proyecto() == null) && entrada.matches("[1-9][0-9]?")) {
                var opciones = anterior != null && anterior.usuario() == usuario ? anterior.respuesta().opciones() : MENU;
                int indice = Integer.parseInt(entrada) - 1;
                if (indice < opciones.size()) entrada = opciones.get(indice);
            }
            boolean menuMostrado = anterior != null && anterior.usuario() == usuario && paso == null;
            Transicion cambio = avanzar(paso, nombre, entrada, menuMostrado, ejecutar);
            var respuesta = cambio.respuesta();
            return new Sesion(usuario, evento, ahora.plus(VIGENCIA), cambio.paso(),
                new TelegramController.Respuesta(respuesta.mensaje(), respuesta.opciones(), evento));
        }).respuesta();
    }

    private Transicion avanzar(Paso paso, String nombre, String texto, boolean menuMostrado, UnaryOperator<String> ejecutar) {
        if (List.of("/start", "/menu", "/cancelar", MENU_PRINCIPAL).contains(texto)) return bienvenida(nombre);
        if (MENU.contains(texto)) return obras(texto, 0);
        if (paso == null) {
            return switch (texto.toLowerCase(Locale.ROOT)) {
                case "restricciones", "/restricciones" -> obras(CONSULTAR, 0);
                case "crear restriccion", "crear restricción", "/nueva_restriccion" -> obras(CREAR, 0);
                case "estado", "estado de obra", "/estado" -> obras(ESTADO, 0);
                default -> menuMostrado && !"hola".equalsIgnoreCase(texto)
                    ? menu("Esa opción no es válida. Elegí una de estas acciones.")
                    : bienvenida(nombre);
            };
        }
        if (paso.proyecto() == null) return seleccionarObra(paso, texto, ejecutar);
        if ("/mensaje_no_admitido".equals(texto)) return pedir(paso,
            paso.descripcion() == null ? "Escribí la descripción de la restricción en texto." : "Escribí la fecha límite como DD/MM/AAAA.");
        if (paso.descripcion() == null) {
            if (texto.isBlank() || texto.length() > 255) return pedir(paso, "Ingresá una descripción de hasta 255 caracteres.");
            return pedir(new Paso(CREAR, 0, List.of(), paso.proyecto(), texto.replaceAll("\\s+", " ")),
                "¿Cuál es la fecha límite? Escribila como DD/MM/AAAA.");
        }
        try {
            LocalDate fecha = texto.contains("/") ? LocalDate.parse(texto, FECHA) : LocalDate.parse(texto);
            if (fecha.isBefore(LocalDate.now(ZONA_OBRA))) return pedir(paso, "La fecha límite no puede ser anterior a hoy. Ingresá otra fecha.");
            String respuesta = ejecutar.apply("/nueva_restriccion " + paso.proyecto() + " " + fecha + " " + paso.descripcion());
            return menu(respuesta);
        } catch (DateTimeParseException ex) {
            return pedir(paso, "La fecha no es válida. Usá DD/MM/AAAA.");
        }
    }

    private Transicion seleccionarObra(Paso paso, String texto, UnaryOperator<String> ejecutar) {
        if (SIGUIENTE.equals(texto)) return obras(paso.accion(), paso.pagina() + 1);
        if (ANTERIOR.equals(texto)) return obras(paso.accion(), Math.max(0, paso.pagina() - 1));
        var elegida = paso.obras().stream().filter(obra -> obra.etiqueta().equals(texto)).findFirst();
        if (elegida.isEmpty()) {
            var opciones = obras(paso.accion(), paso.pagina());
            if (opciones.paso() == null) return opciones;
            return new Transicion(opciones.paso(), new TelegramController.Respuesta(
                "Esa obra no está en la lista.\n\n" + opciones.respuesta().mensaje(), opciones.respuesta().opciones()));
        }
        Long id = elegida.get().id();
        boolean autorizada = proyectos.findById(id).filter(obra -> obra.getEmpresaRelacionada() != null)
            .map(obra -> identidad.canAccessEmpresa(obra.getEmpresaRelacionada().getId())).orElse(false);
        if (!autorizada) return menu("Esa obra ya no está disponible.");
        return switch (paso.accion()) {
            case CONSULTAR -> menu(ejecutar.apply("/restricciones " + id));
            case ESTADO -> menu(estadoDeObra(id));
            default -> pedir(new Paso(CREAR, 0, List.of(), id, null), "¿Qué restricción querés registrar?");
        };
    }

    private Transicion obras(String accion, int solicitada) {
        var disponibles = identidad.isSuperAdmin() ? proyectos.findAll()
            : java.util.stream.Stream.concat(java.util.stream.Stream.of(identidad.empresaId()),
                    empresas.findByCreadoPorUsuarioId(identidad.usuarioId()).stream().map(empresa -> empresa.getId()))
                .distinct().flatMap(id -> proyectos.findByEmpresaRelacionadaId(id).stream()).toList();
        var opciones = disponibles.stream().filter(obra -> obra.getEmpresaRelacionada() != null)
            .sorted(Comparator.comparing(Proyecto::getNombre, String.CASE_INSENSITIVE_ORDER).thenComparing(Proyecto::getId))
            .map(obra -> new Obra(obra.getId(), etiquetaDe(obra))).toList();
        if (opciones.isEmpty()) return menu("No hay obras disponibles para esta cuenta.");
        int pagina = Math.min(solicitada, (opciones.size() - 1) / POR_PAGINA);
        int limite = Math.min((pagina + 1) * POR_PAGINA, opciones.size());
        var visibles = opciones.subList(pagina * POR_PAGINA, limite);
        var botones = new ArrayList<>(visibles.stream().map(Obra::etiqueta).toList());
        if (pagina > 0) botones.add(ANTERIOR);
        if (limite < opciones.size()) botones.add(SIGUIENTE);
        botones.add(MENU_PRINCIPAL);
        return new Transicion(new Paso(accion, pagina, visibles, null, null),
            respuestaConLista("Obras disponibles", List.copyOf(botones)));
    }

    private String estadoDeObra(Long id) {
        var abiertas = restricciones.execute(id, null, ConsultarRestriccionesObraUseCase.Filtro.ABIERTAS, null);
        String actual = "\nRestricciones actuales: " + abiertas.abiertas() + " abiertas · " + abiertas.vencidas() + " vencidas";
        var ultimo = reportes.listar(id).stream().findFirst();
        if (ultimo.isEmpty()) return abiertas.obra() + "\nTodavía no hay un reporte de obra emitido." + actual;
        var reporte = reportes.detalle(id, ultimo.get().id());
        var avance = reporte.snapshot().path("avance").path("obra");
        var horas = reporte.snapshot().path("horas").path("totales");
        var real = avance.path("avance");
        var previsto = avance.path("previsto");
        var desvio = horas.path("desvio");
        var costo = horas.path("costoPrevisto");
        String porcentaje = desvio.isNumber() && costo.isNumber() && costo.asDouble() > 0
            ? String.format(Locale.forLanguageTag("es-AR"), " (%.1f %%)", desvio.asDouble() / costo.asDouble() * 100) : "";
        String plazo = switch (avance.path("plazo").asString()) {
            case "atrasado" -> "Atrasada";
            case "adelantado" -> "Adelantada";
            case "enTermino" -> "En término";
            default -> "Sin datos";
        };
        return abiertas.obra() + "\nÚltimo reporte · corte " + FECHA.format(reporte.fechaCorte())
            + "\nCronograma: " + plazo
            + "\nAvance real / previsto: " + formato(real, " %") + " / " + formato(previsto, " %")
            + "\nHoras reales / previstas: " + formato(horas.path("horasReales"), " HH")
            + " / " + formato(horas.path("horasPrevistas"), " HH")
            + "\nDesvío de mano de obra propia: " + formato(desvio, " ARS") + porcentaje + actual;
    }

    private static String formato(JsonNode valor, String unidad) {
        return valor.isNumber() ? String.format(Locale.forLanguageTag("es-AR"), "%,.2f%s", valor.asDouble(), unidad) : "Sin datos";
    }

    private static String etiquetaDe(Proyecto obra) {
        String nombre = obra.getNombre().replaceAll("\\s+", " ").strip();
        return nombre.substring(0, nombre.offsetByCodePoints(0, Math.min(70, nombre.codePointCount(0, nombre.length()))))
            + " · #" + obra.getId();
    }

    private static Transicion bienvenida(String nombre) {
        String saludo = nombre == null || nombre.isBlank() ? "¡Hola!" : "¡Hola, " + nombre.strip() + "!";
        return menu(saludo + " Soy VortiseBot. Me gustaría ayudarte.");
    }

    private static Transicion menu(String mensaje) {
        return new Transicion(null, respuestaConLista(mensaje, MENU));
    }

    private static TelegramController.Respuesta respuestaConLista(String mensaje, List<String> opciones) {
        var texto = new StringBuilder(mensaje).append("\n\n");
        for (int i = 0; i < opciones.size(); i++) {
            texto.append(i + 1).append(". ").append(opciones.get(i)).append('\n');
        }
        texto.append("\nElegí una opción o escribí su número.");
        return new TelegramController.Respuesta(texto.toString(), opciones);
    }

    private static Transicion pedir(Paso paso, String mensaje) {
        return new Transicion(paso, new TelegramController.Respuesta(mensaje, List.of(MENU_PRINCIPAL)));
    }

    private record Obra(Long id, String etiqueta) { }
    private record Paso(String accion, int pagina, List<Obra> obras, Long proyecto, String descripcion) { }
    private record Sesion(long usuario, long evento, Instant vence, Paso paso, TelegramController.Respuesta respuesta) { }
    private record Transicion(Paso paso, TelegramController.Respuesta respuesta) { }
}
