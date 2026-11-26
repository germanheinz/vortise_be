package com.vortise.gestion.domain.rule.indicadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Carga;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Parametros;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Piso;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Resultado;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.ResultadoSubrubro;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Subrubro;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Tarea;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Dataset and expected values of "Cálculo de indicadores" (05-TEST), criteria CI-01 to CI-08. */
class CalculadoraIndicadoresTest {
    private static final double DELTA = 1e-6;
    private static final Parametros PARAMETROS = new Parametros(5000.0, 4000.0, 8.0);
    private static final Carga PLATEA_3_DIAS = new Carga(1L, null, 3, 10.0, 40.0, 40.0, false);

    private static Subrubro sub(long id, String piso, String nombre, String unidad, double cantidad, double rendimiento,
            Integer personas, List<Carga> cargas) {
        double hh = cantidad * rendimiento;
        return new Subrubro(id, 100L, "Hormigón", piso.hashCode() + 0L, piso, nombre, unidad, cantidad, hh, hh, personas, cargas);
    }

    private static List<Subrubro> obra(List<Carga> cargasPlateaSubsuelo2) {
        List<Subrubro> subrubros = new ArrayList<>();
        subrubros.add(sub(1, "Subsuelo 2", "Platea", "m³", 20, 2.5, 2, cargasPlateaSubsuelo2));
        subrubros.add(sub(2, "Subsuelo 2", "Tabiques", "m²", 60, 0.75, null, List.of()));
        subrubros.add(sub(3, "Subsuelo 2", "Vigas", "ml", 40, 0.4, null, List.of()));
        subrubros.add(sub(4, "Subsuelo 1", "Platea", "m³", 20, 2.5, 2, List.of()));
        subrubros.add(sub(5, "Subsuelo 1", "Tabiques", "m²", 80, 0.75, null, List.of()));
        subrubros.add(sub(6, "Subsuelo 1", "Vigas", "ml", 40, 0.4, null, List.of()));
        subrubros.add(sub(7, "Planta baja", "Vigas", "ml", 40, 0.4, null, List.of()));
        subrubros.add(sub(8, "Planta baja", "Columnas", "m³", 8, 3, null, List.of()));
        return subrubros;
    }

    private static Resultado calcular(List<Carga> cargas) {
        return CalculadoraIndicadores.calcular(obra(cargas), PARAMETROS);
    }

    private static Piso piso(Resultado r, int indice) { return r.rubros().get(0).pisos().get(indice); }
    private static ResultadoSubrubro platea(Resultado r, int piso) { return piso(r, piso).subrubros().get(0); }

    @Test void ci01PresupuestoDeLaPlatea() {
        var presupuesto = platea(calcular(List.of(PLATEA_3_DIAS)), 0).presupuesto();
        assertEquals(100, presupuesto.hhTotales(), DELTA);
        assertEquals(450_000, presupuesto.costo(), DELTA);
        assertEquals(22_500, presupuesto.precioUnitario(), DELTA);
        assertEquals(6.25, presupuesto.diasPrevistos(), DELTA);
        assertEquals(7, presupuesto.diasPrevistosRedondeados());
    }

    @Test void ci02Tarea() {
        Tarea tarea = platea(calcular(List.of(PLATEA_3_DIAS)), 0).tareas().get(0);
        assertEquals(48, tarea.hhPrevistasFecha(), DELTA);
        assertEquals(50, tarea.hhPrevistasHecho(), DELTA);
        assertEquals(80, tarea.hhReales(), DELTA);
        assertEquals(50.0 / 48, tarea.indices().produccion(), DELTA);
        assertEquals("ESTABLE", tarea.indices().estadoProduccion());
        assertEquals(0.625, tarea.indices().productividad(), DELTA);
        assertEquals("BAJA", tarea.indices().estadoProductividad());
        assertEquals(80.0 / 48, tarea.indices().recursos(), DELTA);
        assertEquals("EXCEDIDO", tarea.indices().estadoRecursos());
        assertEquals(1, tarea.personasPorDia());
        assertFalse(tarea.personasPrevistas());
        assertEquals(-135_000, tarea.desvio(), DELTA);
    }

    @Test void ci02RecursosEstablesSonPersonasPrevistas() {
        Tarea tarea = platea(calcular(List.of(new Carga(1L, null, 3, 10.0, 24.0, 24.0, false))), 0).tareas().get(0);
        assertEquals(1.0, tarea.indices().recursos(), DELTA);
        assertTrue(tarea.personasPrevistas());
        assertNull(tarea.personasPorDia());
    }

    @Test void ci02PersonasPorDiaTieneMagnitudMinimaUno() {
        // HR ÷ (d × h) − P = 19,2 ÷ 8 − 2 = 0,4 → +1: fuera de 0,9–1,1 nunca se informa 0.
        Tarea tarea = platea(calcular(List.of(new Carga(1L, null, 1, 3.0, 9.6, 9.6, false))), 0).tareas().get(0);
        assertEquals("EXCEDIDO", tarea.indices().estadoRecursos());
        assertEquals(1, tarea.personasPorDia());
    }

    @Test void ci03Subrubro() {
        ResultadoSubrubro platea = platea(calcular(List.of(PLATEA_3_DIAS)), 0);
        assertTrue(platea.conAvance());
        assertEquals("ESTABLE", platea.indices().estadoProduccion());
        assertEquals(0.625, platea.indices().productividad(), DELTA);
        assertEquals("EXCEDIDO", platea.indices().estadoRecursos());
        assertEquals(4, platea.jornales());
        assertEquals(-135_000, platea.desvio(), DELTA);
        assertEquals(-0.6, platea.desvioPorcentaje(), DELTA);
        assertEquals(-135_000, platea.proyeccion(), DELTA);
    }

    @Test void ci03DosCargasEquivalenAUnaSola() {
        ResultadoSubrubro una = platea(calcular(List.of(PLATEA_3_DIAS)), 0);
        ResultadoSubrubro dos = platea(calcular(List.of(new Carga(1L, null, 2, 6.0, 25.0, 25.0, false),
            new Carga(2L, null, 1, 4.0, 15.0, 15.0, false))), 0);
        assertEquals(una.dias(), dos.dias());
        assertEquals(una.cantidadEjecutada(), dos.cantidadEjecutada(), DELTA);
        assertEquals(una.hhPrevistasFecha(), dos.hhPrevistasFecha(), DELTA);
        assertEquals(una.hhPrevistasHecho(), dos.hhPrevistasHecho(), DELTA);
        assertEquals(una.hhReales(), dos.hhReales(), DELTA);
        assertEquals(una.indices(), dos.indices());
        assertEquals(una.jornales(), dos.jornales());
        assertEquals(una.desvio(), dos.desvio(), DELTA);
        assertEquals(una.proyeccion(), dos.proyeccion(), DELTA);
    }

    @Test void ci03CantidadEjecutadaMayorQueLaPrevistaNoProyecta() {
        ResultadoSubrubro platea = platea(calcular(List.of(new Carga(1L, null, 3, 25.0, 40.0, 40.0, false))), 0);
        assertEquals(0, platea.proyeccion(), DELTA);
    }

    @Test void ci03JornalesRedondeanAlejandoseDelCero() {
        // (HR − HF) ÷ h = (52 − 48) ÷ 8 = 0,5 → 1 ; (44 − 48) ÷ 8 = −0,5 → −1.
        assertEquals(1, platea(calcular(List.of(new Carga(1L, null, 3, 10.0, 26.0, 26.0, false))), 0).jornales());
        assertEquals(-1, platea(calcular(List.of(new Carga(1L, null, 3, 10.0, 22.0, 22.0, false))), 0).jornales());
    }

    @Test void ci04SubrubroSinAvanceUsaLaReferenciaDeOtroPiso() {
        Resultado r = calcular(List.of(PLATEA_3_DIAS));
        ResultadoSubrubro plateaSubsuelo1 = platea(r, 1);
        assertFalse(plateaSubsuelo1.conAvance());
        assertEquals(List.of("Subsuelo 2"), plateaSubsuelo1.referencia().pisos());
        assertEquals(-0.6, plateaSubsuelo1.referencia().porcentaje(), DELTA);
        assertEquals(-270_000, plateaSubsuelo1.proyeccion(), DELTA);

        ResultadoSubrubro tabiquesSubsuelo1 = piso(r, 1).subrubros().get(1);
        assertTrue(tabiquesSubsuelo1.sinReferencia());
        assertEquals(0, tabiquesSubsuelo1.proyeccion(), DELTA);

        var subsuelo1 = piso(r, 1).agregado();
        assertFalse(subsuelo1.conAvance());
        assertEquals(-270_000, subsuelo1.proyeccion(), DELTA);
        assertEquals(-270_000.0 / 1_134_000, subsuelo1.porcentajeAlTerminar(), DELTA);
    }

    @Test void ci05Piso() {
        var subsuelo2 = piso(calcular(List.of(PLATEA_3_DIAS)), 0).agregado();
        assertTrue(subsuelo2.conAvance());
        assertEquals(48, subsuelo2.hhPrevistasFecha(), DELTA);
        assertEquals(50, subsuelo2.hhPrevistasHecho(), DELTA);
        assertEquals(80, subsuelo2.hhReales(), DELTA);
        assertEquals("ESTABLE", subsuelo2.indices().estadoProduccion());
        assertEquals(0.625, subsuelo2.indices().productividad(), DELTA);
        assertEquals("EXCEDIDO", subsuelo2.indices().estadoRecursos());
        assertEquals(80.0 / 48 - 1, subsuelo2.porcentajeHoras(), DELTA);
        assertEquals(-135_000, subsuelo2.desvio(), DELTA);
        assertEquals(-0.6, subsuelo2.desvioPorcentaje(), DELTA);
        assertEquals(999_000, subsuelo2.presupuesto(), DELTA);
        assertEquals(-135_000, subsuelo2.proyeccion(), DELTA);
        assertEquals(-270_000.0 / 999_000, subsuelo2.porcentajeAlTerminar(), DELTA);
    }

    @Test void ci05PisoSinAvanceNoTieneIndices() {
        var plantaBaja = piso(calcular(List.of(PLATEA_3_DIAS)), 2).agregado();
        assertFalse(plantaBaja.conAvance());
        assertNull(plantaBaja.indices().produccion());
        assertNull(plantaBaja.desvio());
        assertEquals(360_000, plantaBaja.presupuesto(), DELTA);
        assertEquals(0, plantaBaja.proyeccion(), DELTA);
    }

    @Test void ci06Rubro() {
        var rubro = calcular(List.of(PLATEA_3_DIAS)).rubros().get(0).agregado();
        assertEquals(2_493_000, rubro.presupuesto(), DELTA);
        assertEquals(0.625, rubro.indices().productividad(), DELTA);
        assertEquals(80.0 / 48, rubro.indices().recursos(), DELTA);
        assertEquals(-135_000, rubro.desvio(), DELTA);
        assertEquals(-0.6, rubro.desvioPorcentaje(), DELTA);
        assertEquals(-405_000, rubro.proyeccion(), DELTA);
        assertEquals(-540_000.0 / 2_493_000, rubro.porcentajeAlTerminar(), DELTA);
    }

    @Test void ci07Obra() {
        var obra = calcular(List.of(PLATEA_3_DIAS)).obra();
        assertEquals(225_000, obra.costoPrevistoHecho(), DELTA);
        assertEquals(360_000, obra.costoReal(), DELTA);
        assertEquals(2_493_000, obra.presupuesto(), DELTA);
        assertEquals(360_000.0 / 2_493_000, obra.porcentajePagado(), DELTA);
        assertEquals(225_000.0 / 2_493_000, obra.porcentajeHecho(), DELTA);
        assertEquals(-135_000, obra.desvio(), DELTA);
        assertEquals(-0.6, obra.desvioPorcentaje(), DELTA);
        assertEquals(-135_000, obra.proyeccionIniciada(), DELTA);
        assertEquals(-270_000.0 / 2_493_000, obra.porcentajeIniciada(), DELTA);
        assertEquals(-270_000, obra.proyeccionSinAvance(), DELTA);
        assertEquals(-405_000, obra.proyeccionTotal(), DELTA);
        assertEquals(-540_000.0 / 2_493_000, obra.porcentajeTotal(), DELTA);
    }

    @Test void ci08DivisionPorCeroEsSinDatos() {
        // Sin personas en el cronograma no hay HF: los índices que dependen de él quedan nulos, nunca NaN.
        Subrubro sinPersonas = sub(1, "Subsuelo 2", "Platea", "m³", 20, 2.5, null, List.of(PLATEA_3_DIAS));
        ResultadoSubrubro r = CalculadoraIndicadores.calcular(List.of(sinPersonas), PARAMETROS).rubros().get(0)
            .pisos().get(0).subrubros().get(0);
        assertNull(r.hhPrevistasFecha());
        assertNull(r.indices().produccion());
        assertNull(r.indices().recursos());
        assertEquals(0.625, r.indices().productividad(), DELTA);
        assertNull(r.jornales());
        assertNull(r.presupuesto().diasPrevistos());

        Resultado sinTarifas = CalculadoraIndicadores.calcular(obra(List.of(PLATEA_3_DIAS)), new Parametros(null, null, 8.0));
        assertNull(sinTarifas.obra().desvio());
        assertNull(sinTarifas.obra().presupuesto());
        assertNull(platea(sinTarifas, 0).proyeccion());
    }

    @Test void ci08LimitesDeEstadoIncluyen0_9Y1_1() {
        assertEquals(Estado.ESTABLE, Estado.de(0.9));
        assertEquals(Estado.ESTABLE, Estado.de(1.1));
        assertEquals(Estado.ESTABLE, Estado.de(1.1000000000000003));
        assertEquals(Estado.BAJO, Estado.de(0.894));
        assertEquals(Estado.ALTO, Estado.de(1.105));
    }

    @Test void cargaDeDesencofradoSumaHorasPeroNoCantidad() {
        ResultadoSubrubro platea = platea(calcular(List.of(PLATEA_3_DIAS, new Carga(2L, null, 1, 5.0, 8.0, 8.0, true))), 0);
        assertEquals(10, platea.cantidadEjecutada(), DELTA);
        assertEquals(96, platea.hhReales(), DELTA);
        assertEquals(4, platea.dias());
    }
}
