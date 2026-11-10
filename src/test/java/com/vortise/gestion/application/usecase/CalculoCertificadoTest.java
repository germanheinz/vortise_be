package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vortise.gestion.application.usecase.CalculoCertificado.Avance;
import com.vortise.gestion.application.usecase.CalculoCertificado.Corte;
import com.vortise.gestion.application.usecase.CalculoCertificado.Fila;
import com.vortise.gestion.application.usecase.CalculoCertificado.Item;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalculoCertificadoTest {
    private static final LocalDate MARZO = LocalDate.of(2026, 3, 31), ABRIL = LocalDate.of(2026, 4, 30), MAYO = LocalDate.of(2026, 5, 31);

    private static Avance avance(String fecha, double cantidad) { return new Avance(1L, LocalDate.parse(fecha), cantidad); }
    private static Corte corte(LocalDate fecha, Long... completados) { return new Corte(fecha, Set.of(completados)); }
    private static Fila fila(List<List<Fila>> result, int certificate) { return result.get(certificate).get(0); }
    private static List<List<Fila>> calcular(double total, List<Avance> avances, Corte... cortes) {
        return CalculoCertificado.calcular(List.of(new Item(1L, total)), avances, List.of(cortes));
    }

    @Test void monthlyExampleFromTheSpecification() {
        var result = calcular(100, List.of(avance("2026-03-10", 6), avance("2026-03-20", 4), avance("2026-04-05", 10)), corte(MARZO), corte(ABRIL));
        assertEquals(0, fila(result, 0).anterior()); assertEquals(10, fila(result, 0).actual()); assertEquals(10, fila(result, 0).acumulado());
        assertEquals(10, fila(result, 1).anterior()); assertEquals(10, fila(result, 1).actual()); assertEquals(20, fila(result, 1).acumulado());
    }

    @Test void newCertificateWithoutLoadsIsAllZero() {
        var row = fila(calcular(100, List.of(), corte(MARZO)), 0);
        assertEquals(0, row.anterior()); assertEquals(0, row.actual()); assertEquals(0, row.acumulado()); assertEquals(0, row.excedente());
        assertEquals(0.0, CalculoCertificado.porcentaje(row.acumulado(), 100.0));
    }

    @Test void aLoadOnTheCutoffDayBelongsToTheCertificateThatClosesAndNeverToTheNext() {
        var result = calcular(100, List.of(avance("2026-03-31", 5), avance("2026-04-01", 7)), corte(MARZO), corte(ABRIL));
        assertEquals(5, fila(result, 0).actual());
        assertEquals(7, fila(result, 1).actual());
        assertEquals(12, fila(result, 1).acumulado());
    }

    @Test void everyLoadIsCountedExactlyOnceAcrossCertificatesAndEditingACutoffMovesLoadsWithoutDuplicating() {
        var loads = List.of(avance("2026-03-30", 3), avance("2026-04-02", 4), avance("2026-04-20", 5));
        var original = calcular(100, loads, corte(MARZO), corte(ABRIL));
        var moved = calcular(100, loads, corte(LocalDate.of(2026, 4, 3)), corte(ABRIL));
        for (var result : List.of(original, moved)) {
            assertEquals(12, fila(result, 0).actual() + fila(result, 1).actual());
            assertEquals(12, fila(result, 1).acumulado());
        }
        assertEquals(3, fila(original, 0).actual()); assertEquals(7, fila(moved, 0).actual()); assertEquals(5, fila(moved, 1).actual());
    }

    @Test void accumulatedIsCappedAtTotalAndTheRestIsInformativeExcess() {
        var result = calcular(100, List.of(avance("2026-03-10", 95), avance("2026-04-10", 8)), corte(MARZO), corte(ABRIL));
        var april = fila(result, 1);
        assertEquals(95, april.anterior()); assertEquals(5, april.actual()); assertEquals(100, april.acumulado());
        assertEquals(3, april.excedente()); assertEquals(8 + 95, april.ejecutado());
        assertEquals(3.0, CalculoCertificado.porcentaje(april.excedente(), 100.0));
        assertEquals(0, fila(result, 0).excedente());
    }

    @Test void excessIsOnlyWhatWasLoadedInThatPeriodAboveTheCapAndNeverCertified() {
        var result = calcular(100, List.of(avance("2026-03-10", 108), avance("2026-04-10", 2)), corte(MARZO), corte(ABRIL), corte(MAYO));
        assertEquals(8, fila(result, 0).excedente()); assertEquals(100, fila(result, 0).acumulado());
        assertEquals(2, fila(result, 1).excedente()); assertEquals(0, fila(result, 1).actual());
        assertEquals(0, fila(result, 2).excedente());
    }

    @Test void completedItemClosesAtTotalWithAdjustmentAndStaysClosedInLaterCertificates() {
        var result = calcular(100, List.of(avance("2026-03-10", 97)), corte(MARZO, 1L), corte(ABRIL));
        var march = fila(result, 0);
        assertEquals(100, march.acumulado()); assertEquals(100, march.actual()); assertEquals(3, march.ajuste());
        assertTrue(march.completado()); assertTrue(march.marcadoEnEste());
        assertEquals(3.0, CalculoCertificado.porcentaje(march.ajuste(), 100.0));
        var april = fila(result, 1);
        assertEquals(100, april.anterior()); assertEquals(0, april.actual()); assertEquals(100, april.acumulado());
        assertEquals(0, april.ajuste()); assertTrue(april.completado()); assertFalse(april.marcadoEnEste());
    }

    @Test void unmarkingRestoresTheRealPercentageAndRemovesTheAdjustment() {
        var row = fila(calcular(100, List.of(avance("2026-03-10", 97)), corte(MARZO)), 0);
        assertEquals(97, row.acumulado()); assertEquals(0, row.ajuste()); assertFalse(row.completado());
    }

    @Test void itemsWithoutQuantityAreListedWithoutCappingOrPercentage() {
        var result = CalculoCertificado.calcular(List.of(new Item(1L, null)), List.of(avance("2026-03-10", 7)), List.of(corte(MARZO)));
        var row = result.get(0).get(0);
        assertEquals(7, row.acumulado()); assertEquals(0, row.excedente());
        assertNull(CalculoCertificado.porcentaje(row.acumulado(), null));
    }

    @Test void loadsOfOtherActivitiesDoNotMix() {
        var items = List.of(new Item(1L, 10.0), new Item(2L, 20.0));
        var result = CalculoCertificado.calcular(items, List.of(new Avance(1L, LocalDate.parse("2026-03-01"), 4), new Avance(2L, LocalDate.parse("2026-03-02"), 6)), List.of(corte(MARZO)));
        assertEquals(4, result.get(0).get(0).acumulado()); assertEquals(6, result.get(0).get(1).acumulado());
    }

    @Test void decimalsDoNotLeakFloatingPointNoise() {
        var row = fila(calcular(100, List.of(avance("2026-03-01", 0.1), avance("2026-03-02", 0.2)), corte(MARZO)), 0);
        assertEquals(0.3, row.acumulado());
    }
}
