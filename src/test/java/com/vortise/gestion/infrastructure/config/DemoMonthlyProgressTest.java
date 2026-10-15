package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import com.vortise.gestion.domain.model.*;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

// Plain objects only: no Spring, repositories, JDBC or application startup.
class DemoMonthlyProgressTest {
    private Proyecto fixture() {
        Proyecto p = new Proyecto();
        p.setNombre("PROYECTO DEMO CRONOGRAMA");
        p.setNumeroProyecto("P-2026-101");
        p.setEmpresaRelacionada(new Empresa("Renamed demo", "DEMO-0001"));
        RubroObra rubro = new RubroObra();
        rubro.setnRubro("5000");
        rubro.setRubro("Hormigón armado");
        rubro.setSubRubro("Fundación");
        rubro.setCantidad(100.0);
        rubro.setFechaInicioPlanificada(LocalDate.of(2026, 8, 1));
        rubro.setFechaFinPlanificada(LocalDate.of(2026, 10, 31));
        rubro.setCronogramaManual(true);
        p.getRubros().add(rubro);
        return p;
    }

    private RegistroHoras record(Double quantity, String description) {
        RegistroHoras r = new RegistroHoras();
        r.setNumeroRubro("5000");
        r.setRubro("Hormigón armado");
        r.setSubRubro("Fundación");
        r.setFin(OffsetDateTime.parse("2026-08-10T16:00:00-03:00"));
        r.setHoras(8.0);
        r.setCantidadReal(quantity);
        r.setDescripcion(description);
        return r;
    }

    @Test void fillsMissingQuantityAndPreservesExistingValuesAndDates() {
        Proyecto p = fixture();
        String marker = "Jornada real de obra: seguimiento de productividad y costo";
        RegistroHoras entered = record(60.0, marker);
        RegistroHoras missing = record(null, marker);
        var end = missing.getFin();
        p.getRegistrosHoras().add(entered);
        p.getRegistrosHoras().add(missing);
        assertTrue(DataSeeder.completeDemoMonthlyProgress(p));
        assertEquals(60.0, entered.getCantidadReal());
        assertEquals(40.0, missing.getCantidadReal());
        assertEquals(end, missing.getFin());
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
        assertEquals(2, p.getRegistrosHoras().size());
    }

    @Test void createsMissingDemoRecordsOnlyOnce() {
        Proyecto p = fixture();
        assertTrue(DataSeeder.completeDemoMonthlyProgress(p));
        assertEquals(4, p.getRegistrosHoras().size());
        assertEquals(100.0, p.getRegistrosHoras().stream().mapToDouble(RegistroHoras::getCantidadReal).sum());
        assertEquals(32.0, p.getHorasReales());
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
        assertEquals(4, p.getRegistrosHoras().size());
    }

    @Test void neverChangesForeignCompanyOrNonDemoProject() {
        Proyecto p = fixture();
        p.setEmpresaRelacionada(new Empresa("Other", "OTHER"));
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
        assertTrue(p.getRegistrosHoras().isEmpty());
        p = fixture();
        p.setNumeroProyecto("OTHER");
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
    }

    @Test void neverCompletesManuallyEnteredRecords() {
        Proyecto p = fixture();
        RegistroHoras manual = record(null, "Manual entry");
        p.getRegistrosHoras().add(manual);
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
        assertNull(manual.getCantidadReal());
        assertEquals(1, p.getRegistrosHoras().size());
    }

    @Test void migratesAugustOnlyFixturesIntoPlannedMonthsWithoutChangingTotals() {
        Proyecto p = fixture();
        OffsetDateTime base = OffsetDateTime.parse("2026-08-03T08:00:00-03:00");
        for (int j = 0; j < 4; j++) {
            RegistroHoras r = record(25.0, "Jornada real de obra: seguimiento de productividad y costo");
            r.setInicio(base.plusDays(j).plusHours((j % 3) * 2L));
            r.setFin(r.getInicio().plusHours(6 + (j % 3) * 2L));
            p.getRegistrosHoras().add(r);
        }
        assertTrue(DataSeeder.completeDemoMonthlyProgress(p));
        var monthly = p.getRegistrosHoras().stream().collect(java.util.stream.Collectors.groupingBy(
            r -> r.getFin().getMonthValue(), java.util.stream.Collectors.summingDouble(RegistroHoras::getCantidadReal)));
        assertEquals(java.util.Map.of(8, 25.0, 9, 25.0, 10, 50.0), monthly);
        assertEquals(100.0, p.getRegistrosHoras().stream().mapToDouble(RegistroHoras::getCantidadReal).sum());
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
    }

    @Test void alteredTimestampPreventsBatchMigration() {
        Proyecto p = fixture();
        OffsetDateTime base = OffsetDateTime.parse("2026-08-03T08:00:00-03:00");
        for (int j = 0; j < 4; j++) {
            RegistroHoras r = record(25.0, "Jornada real de obra: seguimiento de productividad y costo");
            r.setInicio(base.plusDays(j).plusHours((j % 3) * 2L));
            r.setFin(r.getInicio().plusHours(6 + (j % 3) * 2L));
            p.getRegistrosHoras().add(r);
        }
        p.getRegistrosHoras().get(0).setInicio(base.plusMinutes(1));
        var before = p.getRegistrosHoras().stream().map(RegistroHoras::getFin).toList();
        assertFalse(DataSeeder.completeDemoMonthlyProgress(p));
        assertEquals(before, p.getRegistrosHoras().stream().map(RegistroHoras::getFin).toList());
    }

    @Test void allActivitiesHaveDistributedCheckpointsAndExplicitDelays() {
        for (int i = 0; i < 6; i++) {
            LocalDate start = LocalDate.of(2026, 8, 1).plusMonths(i);
            LocalDate end = start.plusMonths(2).minusDays(1);
            var checkpoints = java.util.stream.IntStream.range(0, 4)
                .mapToObj(j -> DataSeeder.demoProgressStart(start, end, 0, j)).toList();
            assertTrue(checkpoints.stream().map(r -> java.time.YearMonth.from(r)).distinct().count() >= 2);
            assertEquals(end.plusDays(i * 3L), DataSeeder.demoProgressStart(start, end, i, 3).toLocalDate());
        }
    }
}
