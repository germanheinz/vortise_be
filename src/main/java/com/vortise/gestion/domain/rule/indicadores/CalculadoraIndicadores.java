package com.vortise.gestion.domain.rule.indicadores;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Indicators of "Cálculo de indicadores" (spec 021) for every level: tarea, subrubro, piso, rubro and obra.
 *
 * <p>Every level adds magnitudes (HH and costs) first and divides afterwards; indices and percentages are never
 * averaged. A missing input or a division by zero yields {@code null}, never NaN. Only own labour enters: callers pass
 * subcontracted subrubros out.
 */
public final class CalculadoraIndicadores {
    private CalculadoraIndicadores() { }

    // ----------------------------------------------------------------------------------------------- input

    public record Parametros(Double costoHoraOficial, Double costoHoraAyudante, Double horasJornada) { }

    /** A progress load. Without days it counts as one day; a formwork-removal load or one without quantity adds hours only. */
    public record Carga(Long id, LocalDate fecha, Integer dias, Double cantidad, Double horasOficiales,
            Double horasAyudantes, boolean desencofrado) { }

    /**
     * A leaf of the structure. {@code piso} is null when its rubro has no floors; {@code personas} already includes
     * the work fronts of the schedule.
     */
    public record Subrubro(Long id, Long rubroId, String rubroNombre, Long nivelId, String piso, String nombre,
            String unidad, Double cantidadPrevista, Double hhOficialesPrevistas, Double hhAyudantesPrevistas,
            Integer personas, List<Carga> cargas) { }

    // ---------------------------------------------------------------------------------------------- output

    public record Indices(Double produccion, String estadoProduccion, Double productividad, String estadoProductividad,
            Double recursos, String estadoRecursos) {
        static final Indices SIN_DATOS = new Indices(null, null, null, null, null, null);
    }

    public record Presupuesto(Double cantidad, Double hhTotales, Double precioUnitario, Double costo, Integer personas,
            Double diasPrevistos, Integer diasPrevistosRedondeados) { }

    /** {@code personasPorDia} is signed and at least 1 in magnitude; {@code personasPrevistas} when resources are stable. */
    public record Tarea(Long tareaId, LocalDate fecha, int dias, double cantidad, Double hhPrevistasFecha,
            Double hhPrevistasHecho, Double hhReales, Indices indices, Integer personasPorDia, boolean personasPrevistas,
            Double costoPrevistoHecho, Double costoReal, Double desvio) { }

    /** Floors whose observed deviation projects a subrubro without progress, and the deviation rate (fraction). */
    public record Referencia(List<String> pisos, Double porcentaje) { }

    public record ResultadoSubrubro(Long rubroId, String nombre, String unidad, boolean conAvance,
            Presupuesto presupuesto, double cantidadEjecutada, int dias, Double hhPrevistasFecha,
            Double hhPrevistasHecho, Double hhReales, Indices indices, Integer jornales, Double costoPrevistoHecho,
            Double costoReal, Double desvio, Double desvioPorcentaje, Double proyeccion, Referencia referencia,
            boolean sinReferencia, List<Tarea> tareas) { }

    /** Floor or rubro: magnitudes of subrubros with progress, budget and projection of all of them. */
    public record Agregado(boolean conAvance, Double hhPrevistasFecha, Double hhPrevistasHecho, Double hhReales,
            Indices indices, Double porcentajeHoras, Double costoPrevistoHecho, Double costoReal, Double presupuesto,
            Double desvio, Double desvioPorcentaje, Double proyeccion, Double porcentajeAlTerminar) { }

    public record Piso(Long nivelId, String nombre, Agregado agregado, List<ResultadoSubrubro> subrubros) { }

    public record Rubro(Long rubroId, String nombre, Agregado agregado, List<Piso> pisos) { }

    /**
     * Whole project. {@code proyeccionIniciada} covers what already started; {@code proyeccionTotal} adds the
     * subrubros without progress projected from other floors ({@code proyeccionSinAvance}).
     */
    public record Obra(Double hhPrevistasFecha, Double hhPrevistasHecho, Double hhReales, Double costoPrevistoHecho,
            Double costoReal, Double presupuesto, Double desvio, Double desvioPorcentaje, Double proyeccionIniciada,
            Double porcentajeIniciada, Double proyeccionSinAvance, Double proyeccionTotal, Double porcentajeTotal,
            Double porcentajePagado, Double porcentajeHecho) { }

    public record Resultado(Obra obra, List<Rubro> rubros) { }

    // ------------------------------------------------------------------------------------------ calculation

    public static Resultado calcular(List<Subrubro> subrubros, Parametros parametros) {
        List<ResultadoSubrubro> calculados = new ArrayList<>();
        for (Subrubro subrubro : subrubros) calculados.add(subrubro(subrubro, parametros));
        calculados = proyectarSinAvance(subrubros, calculados);

        Map<Object, List<Integer>> porRubro = new LinkedHashMap<>();
        for (int i = 0; i < subrubros.size(); i++) {
            porRubro.computeIfAbsent(clave(subrubros.get(i).rubroId()), k -> new ArrayList<>()).add(i);
        }
        List<Rubro> rubros = new ArrayList<>();
        for (List<Integer> indicesRubro : porRubro.values()) {
            Map<String, List<Integer>> porPiso = new LinkedHashMap<>();
            for (int i : indicesRubro) porPiso.computeIfAbsent(normalizar(subrubros.get(i).piso()), k -> new ArrayList<>()).add(i);
            List<Piso> pisos = new ArrayList<>();
            for (List<Integer> indicesPiso : porPiso.values()) {
                Subrubro primero = subrubros.get(indicesPiso.get(0));
                List<ResultadoSubrubro> delPiso = indicesPiso.stream().map(calculados::get).toList();
                boolean nivelUnico = indicesPiso.stream().map(i -> subrubros.get(i).nivelId()).distinct().count() == 1;
                pisos.add(new Piso(nivelUnico ? primero.nivelId() : null, primero.piso(), agregar(delPiso), delPiso));
            }
            Subrubro primero = subrubros.get(indicesRubro.get(0));
            rubros.add(new Rubro(primero.rubroId(), primero.rubroNombre(),
                agregar(indicesRubro.stream().map(calculados::get).toList()), pisos));
        }
        return new Resultado(obra(calculados), rubros);
    }

    static ResultadoSubrubro subrubro(Subrubro s, Parametros p) {
        Presupuesto presupuesto = presupuesto(s, p);
        Double hhPorUnidad = dividir(presupuesto.hhTotales(), positivo(s.cantidadPrevista()));
        Double precioUnitario = presupuesto.precioUnitario();
        Double horasEquipoPorDia = producto(positivo(personasDe(s)), positivo(p.horasJornada()));

        List<Tarea> tareas = new ArrayList<>();
        int dias = 0;
        double cantidad = 0;
        Double hhReales = null;
        Double costoReal = 0.0;
        for (Carga carga : s.cargas()) {
            Tarea tarea = tarea(carga, s, p, hhPorUnidad, precioUnitario, horasEquipoPorDia);
            tareas.add(tarea);
            dias += tarea.dias();
            cantidad += tarea.cantidad();
            hhReales = sumar(hhReales, tarea.hhReales());
            costoReal = costoReal == null || tarea.costoReal() == null ? null : costoReal + tarea.costoReal();
        }
        if (s.cargas().isEmpty() || !tarifas(p)) costoReal = null;

        Double hf = s.cargas().isEmpty() ? null : producto(horasEquipoPorDia, (double) dias);
        Double hg = producto(hhPorUnidad, cantidad);
        boolean conAvance = cantidad > 0 && hhReales != null && hhReales > 0;
        Double costoPrevistoHecho = producto(precioUnitario, cantidad);
        Double desvio = conAvance ? diferencia(costoPrevistoHecho, costoReal) : null;
        Double proyeccion = null;
        if (conAvance && desvio != null && s.cantidadPrevista() != null) {
            proyeccion = finito(desvio / cantidad * Math.max(0, s.cantidadPrevista() - cantidad));
        }
        Integer jornales = conAvance && hf != null && hhReales != null && positivo(p.horasJornada()) != null
            ? Redondeo.alejandoDelCero((hhReales - hf) / p.horasJornada()) : null;
        return new ResultadoSubrubro(s.id(), s.nombre(), s.unidad(), conAvance, presupuesto, cantidad, dias, hf, hg,
            hhReales, conAvance ? indices(hf, hg, hhReales) : Indices.SIN_DATOS, jornales,
            conAvance ? costoPrevistoHecho : null, conAvance ? costoReal : null, desvio,
            dividir(desvio, conAvance ? costoPrevistoHecho : null), proyeccion, null, false, List.copyOf(tareas));
    }

    static Presupuesto presupuesto(Subrubro s, Parametros p) {
        Double of = noNegativo(s.hhOficialesPrevistas());
        Double ay = noNegativo(s.hhAyudantesPrevistas());
        Double hhTotales = of == null || ay == null ? null : finito(of + ay);
        Double costo = of == null || ay == null || !tarifas(p) ? null
            : finito(of * p.costoHoraOficial() + ay * p.costoHoraAyudante());
        Double cantidad = positivo(s.cantidadPrevista());
        Double precioUnitario = dividir(costo, cantidad);
        Integer personas = personasDe(s);
        Double diasPrevistos = dividir(hhTotales, producto(positivo(personas), positivo(p.horasJornada())));
        return new Presupuesto(s.cantidadPrevista(), hhTotales, precioUnitario, costo, personas, diasPrevistos,
            diasPrevistos == null ? null : Redondeo.haciaArriba(diasPrevistos));
    }

    private static Tarea tarea(Carga c, Subrubro s, Parametros p, Double hhPorUnidad, Double precioUnitario,
            Double horasEquipoPorDia) {
        int dias = c.dias() == null || c.dias() < 0 ? 1 : c.dias();
        double cantidad = c.desencofrado() || noNegativo(c.cantidad()) == null ? 0 : c.cantidad();
        Double of = noNegativo(c.horasOficiales());
        Double ay = noNegativo(c.horasAyudantes());
        Double hr = of == null && ay == null ? null : finito(valor(of) + valor(ay));
        Double hf = producto(horasEquipoPorDia, (double) dias);
        Double hg = producto(hhPorUnidad, cantidad);
        Indices indices = indices(hf, hg, hr);

        Integer personasPorDia = null;
        boolean personasPrevistas = false;
        Double horasJornada = positivo(p.horasJornada());
        if (indices.recursos() != null && "SUFICIENTE".equals(indices.estadoRecursos())) {
            personasPrevistas = true;
        } else if (indices.recursos() != null && horasJornada != null && dias > 0) {
            double extra = hr / (dias * horasJornada) - personasDe(s);
            int redondeado = Redondeo.alejandoDelCero(extra);
            personasPorDia = redondeado != 0 ? redondeado : (extra < 0 ? -1 : 1);
        }

        Double costoReal = hr == null || !tarifas(p) ? null
            : finito(valor(of) * p.costoHoraOficial() + valor(ay) * p.costoHoraAyudante());
        Double costoPrevistoHecho = producto(precioUnitario, cantidad);
        return new Tarea(c.id(), c.fecha(), dias, cantidad, hf, hg, hr, indices, personasPorDia, personasPrevistas,
            costoPrevistoHecho, costoReal, diferencia(costoPrevistoHecho, costoReal));
    }

    /**
     * A subrubro without progress takes the deviation rate of the same subrubro (same rubro, name and unit) on the
     * floors that already advanced: Σ deviation ÷ Σ planned cost of what was done, applied to its own budget.
     */
    private static List<ResultadoSubrubro> proyectarSinAvance(List<Subrubro> entradas, List<ResultadoSubrubro> calculados) {
        record Observado(String piso, double previsto, double desvio) { }
        Map<String, List<Observado>> observados = new LinkedHashMap<>();
        for (int i = 0; i < entradas.size(); i++) {
            ResultadoSubrubro r = calculados.get(i);
            Subrubro s = entradas.get(i);
            if (!r.conAvance() || s.piso() == null || r.desvio() == null || r.costoPrevistoHecho() == null
                    || r.costoPrevistoHecho() <= 0) continue;
            observados.computeIfAbsent(claveReferencia(s), k -> new ArrayList<>())
                .add(new Observado(s.piso(), r.costoPrevistoHecho(), r.desvio()));
        }
        List<ResultadoSubrubro> resultado = new ArrayList<>(calculados);
        for (int i = 0; i < entradas.size(); i++) {
            ResultadoSubrubro r = calculados.get(i);
            Subrubro s = entradas.get(i);
            if (r.conAvance()) continue;
            Double costo = r.presupuesto().costo();
            double previsto = 0;
            double desvio = 0;
            List<String> pisos = new ArrayList<>();
            if (s.piso() != null) {
                for (Observado o : observados.getOrDefault(claveReferencia(s), List.of())) {
                    if (normalizar(o.piso()).equals(normalizar(s.piso()))) continue;
                    previsto += o.previsto();
                    desvio += o.desvio();
                    if (pisos.stream().noneMatch(n -> normalizar(n).equals(normalizar(o.piso())))) pisos.add(o.piso());
                }
            }
            Referencia referencia = previsto > 0 ? new Referencia(List.copyOf(pisos), desvio / previsto) : null;
            Double proyeccion = costo == null ? null : referencia == null ? 0.0 : finito(costo * referencia.porcentaje());
            resultado.set(i, new ResultadoSubrubro(r.rubroId(), r.nombre(), r.unidad(), false, r.presupuesto(),
                r.cantidadEjecutada(), r.dias(), r.hhPrevistasFecha(), r.hhPrevistasHecho(), r.hhReales(), r.indices(),
                r.jornales(), r.costoPrevistoHecho(), r.costoReal(), r.desvio(), r.desvioPorcentaje(), proyeccion,
                referencia, referencia == null, r.tareas()));
        }
        return resultado;
    }

    static Agregado agregar(List<ResultadoSubrubro> subrubros) {
        List<ResultadoSubrubro> conAvance = subrubros.stream().filter(ResultadoSubrubro::conAvance).toList();
        Double hf = sumaEstricta(conAvance, ResultadoSubrubro::hhPrevistasFecha);
        Double hg = sumaEstricta(conAvance, ResultadoSubrubro::hhPrevistasHecho);
        Double hr = sumaEstricta(conAvance, ResultadoSubrubro::hhReales);
        Double costoPrevistoHecho = sumaEstricta(conAvance, ResultadoSubrubro::costoPrevistoHecho);
        Double costoReal = sumaEstricta(conAvance, ResultadoSubrubro::costoReal);
        Double presupuesto = sumaEstricta(subrubros, r -> r.presupuesto().costo());
        Double proyeccion = sumaEstricta(subrubros, ResultadoSubrubro::proyeccion);
        Indices indices = conAvance.isEmpty() ? Indices.SIN_DATOS : indices(hf, hg, hr);
        Double desvio = conAvance.isEmpty() ? null : diferencia(costoPrevistoHecho, costoReal);
        return new Agregado(!conAvance.isEmpty(), hf, hg, hr, indices,
            indices.recursos() == null ? null : indices.recursos() - 1, costoPrevistoHecho, costoReal, presupuesto,
            desvio, dividir(desvio, costoPrevistoHecho), proyeccion, alTerminar(desvio, proyeccion, presupuesto));
    }

    static Obra obra(List<ResultadoSubrubro> subrubros) {
        Agregado total = agregar(subrubros);
        Predicate<ResultadoSubrubro> iniciado = ResultadoSubrubro::conAvance;
        Double iniciada = sumaEstricta(subrubros.stream().filter(iniciado).toList(), ResultadoSubrubro::proyeccion);
        List<ResultadoSubrubro> pendientes = subrubros.stream().filter(iniciado.negate()).toList();
        Double sinAvance = pendientes.isEmpty() ? Double.valueOf(0.0) : sumaEstricta(pendientes, ResultadoSubrubro::proyeccion);
        return new Obra(total.hhPrevistasFecha(), total.hhPrevistasHecho(), total.hhReales(), total.costoPrevistoHecho(),
            total.costoReal(), total.presupuesto(), total.desvio(), total.desvioPorcentaje(), iniciada,
            alTerminar(total.desvio(), iniciada, total.presupuesto()), sinAvance, total.proyeccion(),
            total.porcentajeAlTerminar(), dividir(total.costoReal(), total.presupuesto()),
            dividir(total.costoPrevistoHecho(), total.presupuesto()));
    }

    static Indices indices(Double hf, Double hg, Double hr) {
        Double produccion = dividir(hg, hf);
        Double productividad = dividir(hg, hr);
        Double recursos = dividir(hr, hf);
        return new Indices(produccion, Estado.produccion(produccion), productividad, Estado.productividad(productividad),
            recursos, Estado.recursos(recursos));
    }

    // -------------------------------------------------------------------------------------------- helpers

    /** Deviation plus projection over the budget; a level without progress has no deviation yet (counts as 0). */
    private static Double alTerminar(Double desvio, Double proyeccion, Double presupuesto) {
        if (proyeccion == null) return null;
        return dividir(valor(desvio) + proyeccion, presupuesto);
    }

    private static Integer personasDe(Subrubro s) {
        return s.personas() != null && s.personas() > 0 ? s.personas() : null;
    }

    private static String claveReferencia(Subrubro s) {
        return clave(s.rubroId()) + "|" + normalizar(s.nombre()) + "|" + normalizar(s.unidad());
    }

    private static String clave(Long id) {
        return id == null ? "" : id.toString();
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").trim()
            .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean tarifas(Parametros p) {
        return noNegativo(p.costoHoraOficial()) != null && noNegativo(p.costoHoraAyudante()) != null;
    }

    private static <T> Double sumaEstricta(List<T> items, Function<T, Double> campo) {
        if (items.isEmpty()) return null;
        double total = 0;
        for (T item : items) {
            Double v = campo.apply(item);
            if (v == null) return null;
            total += v;
        }
        return finito(total);
    }

    private static Double sumar(Double acumulado, Double valor) {
        if (valor == null) return acumulado;
        return acumulado == null ? valor : finito(acumulado + valor);
    }

    private static Double dividir(Double numerador, Double denominador) {
        if (numerador == null || denominador == null || denominador <= 0) return null;
        return finito(numerador / denominador);
    }

    private static Double producto(Double a, Double b) {
        return a == null || b == null ? null : finito(a * b);
    }

    private static Double producto(Double a, Integer b) {
        return b == null ? null : producto(a, b.doubleValue());
    }

    private static Double diferencia(Double a, Double b) {
        return a == null || b == null ? null : finito(a - b);
    }

    private static Double positivo(Double v) {
        return v != null && Double.isFinite(v) && v > 0 ? v : null;
    }

    private static Double positivo(Integer v) {
        return v != null && v > 0 ? v.doubleValue() : null;
    }

    private static Double noNegativo(Double v) {
        return v != null && Double.isFinite(v) && v >= 0 ? v : null;
    }

    private static double valor(Double v) {
        return Objects.requireNonNullElse(v, 0.0);
    }

    private static Double finito(double v) {
        return Double.isFinite(v) ? v : null;
    }
}
