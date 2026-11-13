package com.vortise.gestion.domain.service;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.model.VarianteEstructura;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Domain rules of the 3-layer activity structure (rubro, nivel, subrubro).
 *
 * <p>The client sends hierarchical codes ({@code 1}, {@code 1.1}, {@code 1.1.1}); the server only validates them
 * (format, uniqueness per project, consistency with the parent) and never renumbers. This class is the single place
 * that replaces the former implicit {@code code % 100} hierarchy: it validates the tree, tells leaves from grouping
 * nodes, derives the legacy {@code rubro/subRubro/nivel} columns and gives the natural code order.
 *
 * <p>Payloads without {@code tipoNodo} (legacy clients) are interpreted as before: a numeric code that is a multiple
 * of 100 is a root rubro and any other code is a leaf subrubro under {@code code / 100 * 100} when that root exists in
 * the same list. Codes that are not hierarchical with dots are accepted as they come.
 */
public final class EstructuraRubros {
    public static final int PROFUNDIDAD_MAXIMA = 3;
    private static final int LARGO_MAXIMO_NOMBRE = 255;
    private static final String SEGMENTO = "[1-9]\\d{0,8}";
    private static final Pattern CODIGO_FORMATO = Pattern.compile("\\d+(\\.\\d+){0,2}");
    private static final Pattern CODIGO_ESTRICTO = Pattern.compile(SEGMENTO + "(\\." + SEGMENTO + "){0,2}");
    private static final Pattern NUMERICO_LEGADO = Pattern.compile("\\d{1,9}");
    private static final Pattern CODIGO_ORDENABLE = Pattern.compile("\\d{1,18}(\\.\\d{1,18})*");

    /** Natural order of codes: segment by segment as numbers, so {@code 1.2 < 1.10 < 2}; blank codes go last. */
    public static final Comparator<String> ORDEN_CODIGO = EstructuraRubros::compararCodigos;
    /** Natural order of rubros by code. */
    public static final Comparator<RubroObra> ORDEN_NATURAL = Comparator.comparing(RubroObra::getnRubro, ORDEN_CODIGO);

    private EstructuraRubros() { }

    /** One requested node, as sent by the client plus what is already stored for the same id (if any). */
    public record Entrada(String codigo, String parentCodigo, String tipoNodo, String variante, String nombre,
            String rubro, String subRubro, String nivel, TipoNodoRubro tipoPrevio, VarianteEstructura variantePrevia) { }

    /** A validated node. {@code indice} is the position of the matching {@link Entrada}. */
    public record Nodo(int indice, String codigo, String parentCodigo, TipoNodoRubro tipo, VarianteEstructura variante,
            boolean hoja, int capa, String nombre, String rubro, String subRubro, String nivel) { }

    // ------------------------------------------------------------------ codes

    /** Kind inferred from a legacy numeric code: multiples of 100 are root rubros, anything else is a subrubro. */
    public static TipoNodoRubro tipoLegado(String codigo) {
        Long valor = numerico(codigo);
        return valor != null && valor % 100 == 0 ? TipoNodoRubro.RUBRO : TipoNodoRubro.SUBRUBRO;
    }

    /** Depth of a well formed code: {@code 1} is 1, {@code 1.1} is 2 and {@code 1.1.1} is 3. */
    public static int profundidad(String codigo) {
        int dots = 0;
        for (int i = 0; i < codigo.length(); i++) if (codigo.charAt(i) == '.') dots++;
        return dots + 1;
    }

    /** Code of the parent ({@code 1.2.3 -> 1.2}); null for a root code. */
    public static String codigoPadre(String codigo) {
        if (codigo == null) return null;
        int last = codigo.lastIndexOf('.');
        return last < 0 ? null : codigo.substring(0, last);
    }

    /** Compares codes segment by segment as numbers; null/blank codes last; non numeric codes after numeric ones. */
    public static int compararCodigos(String a, String b) {
        String x = texto(a), y = texto(b);
        if (x == null && y == null) return 0;
        if (x == null) return 1;
        if (y == null) return -1;
        long[] sx = segmentos(x), sy = segmentos(y);
        if (sx == null && sy == null) return x.compareTo(y);
        if (sx == null) return 1;
        if (sy == null) return -1;
        for (int i = 0; i < Math.min(sx.length, sy.length); i++) {
            int cmp = Long.compare(sx[i], sy[i]);
            if (cmp != 0) return cmp;
        }
        return Integer.compare(sx.length, sy.length);
    }

    // ------------------------------------------------- tree over stored rubros

    /** True when some other node of {@code todos} has {@code nodo} as its parent. */
    public static boolean tieneHijos(RubroObra nodo, Collection<RubroObra> todos) {
        if (nodo == null || todos == null) return false;
        for (RubroObra otro : todos) {
            if (otro != nodo && otro.getParent() != null && mismo(otro.getParent(), nodo)) return true;
        }
        return false;
    }

    /**
     * A leaf is the only node that is an activity: it is not a root rubro, has no children and sits in the last layer
     * of its variant. Rows without a resolvable parent (legacy data) count as leaves when they have no children.
     */
    public static boolean esHoja(RubroObra nodo, Collection<RubroObra> todos) {
        if (nodo == null || nodo.getTipoNodo() == TipoNodoRubro.RUBRO || tieneHijos(nodo, todos)) return false;
        RubroObra raiz = raizEstructural(nodo);
        if (raiz == null) return true;
        VarianteEstructura variante = raiz.getVarianteEstructura();
        return variante == null || capa(nodo) >= variante.profundidad();
    }

    /** Leaves of the structure, in the given order. */
    public static List<RubroObra> hojas(Collection<RubroObra> todos) {
        List<RubroObra> result = new ArrayList<>();
        if (todos == null) return result;
        for (RubroObra nodo : todos) if (esHoja(nodo, todos)) result.add(nodo);
        return result;
    }

    /** Layer of a node following its parent links (1 for roots and orphans). */
    public static int capa(RubroObra nodo) {
        int capa = 1;
        RubroObra actual = nodo.getParent();
        while (actual != null && capa <= PROFUNDIDAD_MAXIMA) {
            capa++;
            actual = actual.getParent();
        }
        return capa;
    }

    /**
     * Root rubro that groups a node: the root of its parent chain or, for legacy rows without parent link, the root
     * whose numeric code is {@code code / 100 * 100}. Null when the node has no root.
     */
    public static RubroObra raiz(RubroObra nodo, Collection<RubroObra> todos) {
        if (nodo == null) return null;
        RubroObra estructural = raizEstructural(nodo);
        if (estructural != null) return estructural;
        if (todos == null) return null;
        Long codigo = numerico(nodo.getnRubro());
        if (codigo == null) return null;
        long codigoRaiz = codigo / 100 * 100;
        for (RubroObra candidato : todos) {
            Long valor = numerico(candidato.getnRubro());
            if (candidato != nodo && candidato.getTipoNodo() == TipoNodoRubro.RUBRO && valor != null && valor == codigoRaiz) return candidato;
        }
        return null;
    }

    /** Intermediate groupers between the root rubro and the node (outermost first); empty for 2-layer and legacy rows. */
    public static List<RubroObra> intermedios(RubroObra nodo) {
        List<RubroObra> result = new ArrayList<>();
        RubroObra actual = nodo.getParent();
        int guarda = 0;
        while (actual != null && actual.getTipoNodo() != TipoNodoRubro.RUBRO && guarda++ <= PROFUNDIDAD_MAXIMA) {
            result.add(0, actual);
            actual = actual.getParent();
        }
        return result;
    }

    /** Name of the node: {@code nombre}, falling back to the legacy columns. */
    public static String nombreDe(RubroObra nodo) {
        String nombre = texto(nodo.getNombre());
        if (nombre != null) return nombre;
        if (nodo.getTipoNodo() == TipoNodoRubro.RUBRO) return texto(nodo.getRubro());
        String sub = texto(nodo.getSubRubro());
        return sub != null ? sub : texto(nodo.getRubro());
    }

    private static RubroObra raizEstructural(RubroObra nodo) {
        RubroObra actual = nodo;
        int guarda = 0;
        while (actual.getParent() != null && guarda++ <= PROFUNDIDAD_MAXIMA) actual = actual.getParent();
        return actual.getTipoNodo() == TipoNodoRubro.RUBRO ? actual : null;
    }

    private static boolean mismo(RubroObra a, RubroObra b) {
        if (a == b) return true;
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    // ------------------------------------------------------ request validation

    /**
     * Validates the requested tree and resolves kind, parent, leaf flag and the derived legacy columns of each node.
     * Rules: depth up to 3, kind by layer according to the root variant, parent inside the same list, unique code and
     * code consistent with the parent ({@code parent + "." + positive integer}; roots are positive integers).
     *
     * @throws SolicitudInvalidaException when the tree is invalid
     */
    public static List<Nodo> resolver(List<Entrada> entradas) {
        List<Entrada> lista = entradas == null ? List.of() : entradas;
        int n = lista.size();
        String[] codigo = new String[n];
        Map<String, Integer> indicePorCodigo = new HashMap<>();
        for (int i = 0; i < n; i++) {
            codigo[i] = texto(lista.get(i).codigo());
            if (codigo[i] != null && indicePorCodigo.put(codigo[i], i) != null) {
                throw invalida("El código de actividad " + codigo[i] + " está repetido en la obra");
            }
        }

        TipoNodoRubro[] tipo = new TipoNodoRubro[n];
        boolean[] explicito = new boolean[n];
        VarianteEstructura[] variante = new VarianteEstructura[n];
        for (int i = 0; i < n; i++) {
            Entrada e = lista.get(i);
            if (texto(e.tipoNodo()) != null) {
                tipo[i] = tipoDe(e.tipoNodo());
                explicito[i] = true;
            } else if (e.tipoPrevio() != null) {
                tipo[i] = e.tipoPrevio();
            } else if (!conPuntos(codigo[i])) {
                tipo[i] = tipoLegado(codigo[i]);
            }
            if (tipo[i] == TipoNodoRubro.RUBRO) variante[i] = varianteDe(e.variante(), e.variantePrevia());
        }
        // Dotted codes without kind (legacy client): the kind follows the layer of the variant of their root.
        for (int i = 0; i < n; i++) {
            if (tipo[i] != null) continue;
            validarFormato(codigo[i]);
            String codigoRaiz = codigo[i].substring(0, codigo[i].indexOf('.'));
            Integer raiz = indicePorCodigo.get(codigoRaiz);
            VarianteEstructura v = raiz != null && tipo[raiz] == TipoNodoRubro.RUBRO ? variante[raiz] : VarianteEstructura.RUBRO_SUBRUBRO;
            int profundidad = profundidad(codigo[i]);
            if (profundidad > v.profundidad()) throw invalida("La variante " + v + " admite hasta " + v.profundidad() + " capas");
            tipo[i] = v.tipoEnCapa(profundidad);
        }

        String[] padre = new String[n];
        int[] padreIdx = new int[n];
        for (int i = 0; i < n; i++) {
            padreIdx[i] = -1;
            Entrada e = lista.get(i);
            String solicitado = texto(e.parentCodigo());
            if (tipo[i] == TipoNodoRubro.RUBRO) {
                if (explicito[i] && solicitado != null) throw invalida("Un rubro no puede tener parentCodigo");
                continue;
            }
            String pc = solicitado;
            if (pc == null) {
                if (explicito[i]) throw invalida("parentCodigo es obligatorio para un nodo " + tipo[i]);
                if (conPuntos(codigo[i])) {
                    validarFormato(codigo[i]);
                    pc = codigoPadre(codigo[i]);
                } else {
                    Long valor = numerico(codigo[i]);
                    String candidato = valor == null ? null : String.valueOf(valor / 100 * 100);
                    Integer indice = candidato == null ? null : indicePorCodigo.get(candidato);
                    if (indice != null && indice != i && tipo[indice] == TipoNodoRubro.RUBRO) pc = candidato;
                }
            }
            if (pc == null) continue; // legacy orphan: accepted as it comes
            Integer indice = indicePorCodigo.get(pc);
            if (indice == null) throw invalida("El parentCodigo " + pc + " no existe en la obra");
            if (indice == i) throw invalida("Un nodo no puede ser su propio padre");
            padre[i] = pc;
            padreIdx[i] = indice;
        }

        int[] capa = new int[n];
        int[] cima = new int[n];
        for (int i = 0; i < n; i++) {
            int actual = i;
            int c = 1;
            while (padreIdx[actual] >= 0) {
                actual = padreIdx[actual];
                if (++c > PROFUNDIDAD_MAXIMA) throw invalida("La estructura admite como máximo " + PROFUNDIDAD_MAXIMA + " capas");
            }
            capa[i] = c;
            cima[i] = actual;
        }
        for (int i = 0; i < n; i++) {
            if (tipo[i] == TipoNodoRubro.RUBRO || padreIdx[i] < 0) continue;
            int raiz = cima[i];
            if (tipo[raiz] != TipoNodoRubro.RUBRO) throw invalida("El nodo " + codigo[i] + " debe pertenecer a un rubro");
            VarianteEstructura v = variante[raiz];
            if (capa[i] > v.profundidad()) throw invalida("La variante " + v + " admite hasta " + v.profundidad() + " capas");
            if (tipo[i] != v.tipoEnCapa(capa[i])) {
                throw invalida("Un nodo " + tipo[i] + " no corresponde a la capa " + capa[i] + " de la variante " + v);
            }
        }

        for (int i = 0; i < n; i++) {
            if (!explicito[i] && !conPuntos(codigo[i])) continue; // legacy plain codes
            if (codigo[i] == null) throw invalida("El código de la actividad es obligatorio");
            validarFormato(codigo[i]);
            if (!CODIGO_ESTRICTO.matcher(codigo[i]).matches()) {
                throw invalida("Cada segmento del código " + codigo[i] + " debe ser un entero positivo sin ceros a la izquierda");
            }
            if (tipo[i] == TipoNodoRubro.RUBRO) {
                if (profundidad(codigo[i]) != 1) throw invalida("El código de un rubro debe ser un entero positivo");
            } else if (padre[i] == null || !codigo[i].startsWith(padre[i] + ".")
                    || codigo[i].indexOf('.', padre[i].length() + 1) >= 0) {
                throw invalida("El código " + codigo[i] + " no es consistente con su padre " + padre[i]);
            }
        }

        boolean[] conHijos = new boolean[n];
        for (int i = 0; i < n; i++) if (padreIdx[i] >= 0) conHijos[padreIdx[i]] = true;

        String[] nombre = new String[n];
        for (int i = 0; i < n; i++) {
            Entrada e = lista.get(i);
            String propio = texto(e.nombre());
            if (explicito[i] && propio == null) throw invalida("El nombre de la actividad es obligatorio");
            if (propio != null && propio.length() > LARGO_MAXIMO_NOMBRE) {
                throw invalida("El nombre de la actividad supera los " + LARGO_MAXIMO_NOMBRE + " caracteres");
            }
            if (propio != null) nombre[i] = propio;
            else if (tipo[i] == TipoNodoRubro.RUBRO) nombre[i] = texto(e.rubro());
            else nombre[i] = firstText(e.subRubro(), e.rubro());
        }

        List<Nodo> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Entrada e = lista.get(i);
            boolean raizPropia = tipo[i] == TipoNodoRubro.RUBRO;
            boolean enEstructura = raizPropia || padreIdx[i] >= 0;
            VarianteEstructura v = raizPropia ? variante[i] : enEstructura ? variante[cima[i]] : null;
            boolean hoja = !raizPropia && !conHijos[i] && (v == null || capa[i] >= v.profundidad());
            String propio = texto(e.nombre());

            String rubro;
            if (raizPropia) rubro = nombre[i];
            else if (enEstructura && nombre[cima[i]] != null) rubro = nombre[cima[i]];
            else rubro = e.rubro();

            String subRubro = null;
            String nivel = null;
            if (hoja) {
                subRubro = propio != null ? propio : e.subRubro();
                nivel = e.nivel();
                if (v != null && v.tiene(TipoNodoRubro.NIVEL)) {
                    String delNivel = nombreAncestroNivel(i, tipo, padreIdx, nombre);
                    if (delNivel != null) nivel = delNivel;
                }
            }
            result.add(new Nodo(i, codigo[i], padre[i], tipo[i], raizPropia ? variante[i] : null, hoja, capa[i],
                nombre[i], rubro, subRubro, nivel));
        }
        return result;
    }

    private static String nombreAncestroNivel(int indice, TipoNodoRubro[] tipo, int[] padreIdx, String[] nombre) {
        int actual = indice;
        for (int paso = 0; actual >= 0 && paso <= PROFUNDIDAD_MAXIMA; paso++) {
            if (tipo[actual] == TipoNodoRubro.NIVEL) return nombre[actual];
            actual = padreIdx[actual];
        }
        return null;
    }

    private static TipoNodoRubro tipoDe(String valor) {
        try {
            return TipoNodoRubro.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalida("tipoNodo inválido: " + valor + " (RUBRO, NIVEL o SUBRUBRO)");
        }
    }

    private static VarianteEstructura varianteDe(String valor, VarianteEstructura previa) {
        if (texto(valor) == null) return previa != null ? previa : VarianteEstructura.RUBRO_SUBRUBRO;
        try {
            return VarianteEstructura.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalida("varianteEstructura inválida: " + valor);
        }
    }

    private static void validarFormato(String codigo) {
        if (codigo == null || !CODIGO_FORMATO.matcher(codigo).matches()) {
            throw invalida("Código con formato inválido: " + codigo + " (ej. 1, 1.1, 1.1.1)");
        }
    }

    private static boolean conPuntos(String codigo) {
        return codigo != null && codigo.indexOf('.') >= 0;
    }

    private static Long numerico(String codigo) {
        String valor = texto(codigo);
        return valor != null && NUMERICO_LEGADO.matcher(valor).matches() ? Long.valueOf(valor) : null;
    }

    private static long[] segmentos(String codigo) {
        if (!CODIGO_ORDENABLE.matcher(codigo).matches()) return null;
        String[] partes = codigo.split("\\.");
        long[] result = new long[partes.length];
        for (int i = 0; i < partes.length; i++) result[i] = Long.parseLong(partes[i]);
        return result;
    }

    private static String firstText(String first, String second) {
        String value = texto(first);
        return value != null ? value : texto(second);
    }

    private static String texto(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static SolicitudInvalidaException invalida(String mensaje) {
        return new SolicitudInvalidaException(mensaje);
    }
}
