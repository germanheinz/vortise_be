package com.vortise.gestion.application.usecase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrevistoRealCalculatorTest {
    private PrevistoRealCalculator.Metrics calculate(double q, double of, double ay) {
        return PrevistoRealCalculator.calculate(155.4, 4000., 1881.5, q, of, ay, 2000., 1000., 8.);
    }
    @Test void excelTaskOne() {
        var m = calculate(6, 100, 80);
        assertEquals(1.2440703902, m.produccion(), 1e-8);
        assertEquals(1.2615830116, m.productividad(), 1e-8);
        assertEquals(5.8856177606, m.recurso(), 1e-8);
        assertEquals(101525.096525, m.desvio(), 1e-5);
        assertEquals(2527974.903475, m.proyeccion(), 1e-5);
        assertEquals("RAPIDA", m.estadoProduccion());
        assertEquals("ALTA", m.estadoProductividad());
        assertEquals("EXCEDIDO", m.estadoRecurso());
    }
    @Test void excelTaskTwo() {
        var m = calculate(5, 100, 80);
        assertEquals(.2440703902, m.produccion(), 1e-8);
        assertEquals(1.0513191763, m.productividad(), 1e-8);
        assertEquals(1.1546814672, m.recurso(), 1e-8);
        assertEquals(37937.580438, m.desvio(), 1e-5);
        assertEquals(1141162.419562, m.proyeccion(), 1e-5);
        assertEquals("ESTABLE", m.estadoProduccion());
        assertEquals("ESTABLE", m.estadoProductividad());
    }
    @Test void excelAggregateRecalculatesRatherThanAddingProjections() {
        var m = calculate(11, 200, 160);
        assertEquals(1.4881407804, m.produccion(), 1e-8);
        assertEquals(1.156451094, m.productividad(), 1e-8);
        assertEquals(7.0402992278, m.recurso(), 1e-8);
        assertEquals(139462.676963, m.desvio(), 1e-5);
        assertEquals(1830764.595765, m.proyeccion(), 1e-5);
        assertNotEquals(calculate(6,100,80).proyeccion() + calculate(5,100,80).proyeccion(), m.proyeccion());
    }
    @Test void missingInputsDoNotInventSavings() {
        var m = PrevistoRealCalculator.calculate(155.4,4000.,1881.5,6.,100.,80.,null,1000.,null);
        assertNull(m.desvio()); assertNull(m.proyeccion()); assertNull(m.recurso());
        assertNotNull(m.productividad());
        var absent = PrevistoRealCalculator.calculate(155.4,4000.,1881.5,null,null,null,2000.,1000.,8.);
        assertNull(absent.produccion()); assertNull(absent.desvio());
    }
    @Test void zeroProductionStillRecordsCostButHasNoProjectionOrProductivity() {
        var m = calculate(0,100,80);
        assertEquals(-280000.,m.desvio()); assertNull(m.proyeccion()); assertNull(m.productividad());
        assertEquals("LENTA",m.estadoProduccion()); assertEquals("INSUFICIENTE",m.estadoRecurso());
    }
    @Test void zeroHoursAndOvercompletionAreSafe() {
        assertNull(calculate(1,0,0).productividad());
        assertEquals(0.,calculate(160,100,80).proyeccion());
        assertNull(PrevistoRealCalculator.calculate(0.,0.,0.,1.,1.,1.,1.,1.,8.).produccion());
    }
    @Test void invalidNumbersNeverBecomeValidIndicators() {
        for (Double value : new Double[]{Double.NaN,Double.POSITIVE_INFINITY,-1.}) {
            var m = calculate(value,100,80);
            assertNull(m.produccion()); assertNull(m.desvio()); assertNull(m.productividad());
        }
    }
    @Test void thresholdsMatchExcelAtBoundaries() {
        var m = PrevistoRealCalculator.calculate(10.,10.,0.,1.2,1.,0.,1.,1.,1.);
        assertEquals("ESTABLE",m.estadoProductividad());
        assertEquals("ESTABLE",m.estadoProduccion());
        assertEquals("SUFICIENTE",m.estadoRecurso());
    }
}
