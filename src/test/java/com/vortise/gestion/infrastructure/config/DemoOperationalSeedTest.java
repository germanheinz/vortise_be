package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.application.usecase.PrevistoRealCalculator;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure entity fixtures: no context, database, JDBC or file operations. */
class DemoOperationalSeedTest {
    @Test void removesSupersededEmptyDemoPlaceholdersWithoutAddingProduction() {
        var p=fixture(); DemoOperationalSeed.complete(p,8);
        int count=p.getObraTareas().size(); double hours=p.getHorasReales();
        String[] names={"Corte de soga","Apertura de accesos","Replanteo de eje"};
        double[] percentages={94.,88.,91.};
        for(int i=0;i<3;i++) {
            var t=new ObraTarea(); t.setNombre(names[i]); t.setRubro(p.getRubros().get(i).getRubro());
            t.setUnidad("m2"); t.setPorcentajeCumplimiento(percentages[i]);
            t.setComentario(DemoOperationalSeed.LEGACY_NOTE); p.getObraTareas().add(t);
        }
        assertTrue(DemoOperationalSeed.complete(p,8));
        assertEquals(count,p.getObraTareas().size()); assertEquals(hours,p.getHorasReales(),1e-7);
        assertTrue(p.getObraTareas().stream().allMatch(t->t.getRubroId()!=null));
        assertFalse(DemoOperationalSeed.complete(p,8));
    }

    @Test void existingProductionDoesNotCauseManualUnlinkedTasksToBeDeleted() {
        var p=fixture(); DemoOperationalSeed.complete(p,8);
        var t=new ObraTarea(); t.setNombre("Carga manual"); t.setRubro(p.getRubros().get(0).getRubro());
        p.getObraTareas().add(t);
        DemoOperationalSeed.complete(p,8);
        assertTrue(p.getObraTareas().contains(t)); assertNull(t.getRubroId());
    }
    static Proyecto fixture() {
        var p=new Proyecto(); p.setNombre("PROYECTO DEMO CRONOGRAMA"); p.setNumeroProyecto("P-2026-101");
        p.setEmpresaRelacionada(new Empresa("Demo renamed","DEMO-0001"));
        p.setFechaInicio(LocalDate.of(2026,8,1)); p.setFechaFin(LocalDate.of(2027,2,28));
        String[] names={"Hormigón armado","Vaciado de losas","Mampostería","Pintura","Instalaciones sanitarias","Instalaciones eléctricas"};
        for(int i=0;i<6;i++) {
            var r=new RubroObra(); r.setId((long)i+1); r.setProyecto(p); r.setnRubro(String.valueOf(5000+i)); r.setRubro(names[i]);
            r.setCantidad(155.4); r.setHorasOficialesPrevistas(4000.); r.setHorasAyudantesPrevistas(1881.5);
            p.getRubros().add(r);
        }
        return p;
    }
    @Test void allActivitiesHaveDatedDailyLoadsBoundedByCrewCapacityAndCutoff() {
        var p=fixture(); assertTrue(DemoOperationalSeed.complete(p,8));
        assertEquals(6,p.getObraTareas().stream().map(ObraTarea::getRubroId).distinct().count());
        boolean savings=false,overrun=false;
        for(var r:p.getRubros()) {
            var loads=p.getObraTareas().stream().filter(t -> r.getId().equals(t.getRubroId())).toList();
            assertFalse(loads.isEmpty());
            assertEquals(loads.size(),loads.stream().map(ObraTarea::getFecha).distinct().count());
            double q=0,of=0,ay=0;
            for(var t:loads) {
                assertFalse(t.getFecha().isBefore(r.getFechaInicioPlanificada())); assertFalse(t.getFecha().isAfter(p.getFechaFin()));
                assertNotEquals(DayOfWeek.SATURDAY,t.getFecha().getDayOfWeek()); assertNotEquals(DayOfWeek.SUNDAY,t.getFecha().getDayOfWeek());
                assertTrue(t.getHorasOficiales()+t.getHorasAyudantes()<=r.getCantidadPersonas()*8+1e-8);
                assertTrue(t.getCantidadReal()>0); assertNotNull(t.getDescripcion()); assertNotNull(t.getPorcentajeCumplimiento());
                assertEquals(r.getUnidad(),t.getUnidad()); assertEquals(r.getNivel(),t.getNivel());
                q+=t.getCantidadReal();of+=t.getHorasOficiales();ay+=t.getHorasAyudantes();
            }
            assertTrue(q<=r.getCantidad()+1e-8);
            var m=PrevistoRealCalculator.calculate(r.getCantidad(),r.getHorasOficialesPrevistas(),r.getHorasAyudantesPrevistas(),q,of,ay,2000.,1000.,8.);
            assertNotNull(m.produccion());assertNotNull(m.productividad());assertNotNull(m.recurso());assertNotNull(m.desvio());assertNotNull(m.proyeccion());
            savings|=m.desvio()>0;overrun|=m.desvio()<0;
        }
        assertTrue(savings);assertTrue(overrun);
        assertTrue(p.getObraTareas().stream().map(t -> java.time.YearMonth.from(t.getFecha())).distinct().count()>3);
    }
    @Test void rerunIsIdempotentAndPreservesEditedGeneratedLoad() {
        var p=fixture(); DemoOperationalSeed.complete(p,8);
        int tasks=p.getObraTareas().size(),logs=p.getRegistrosHoras().size();
        var first=p.getObraTareas().get(0); first.setCantidadReal(.123);
        assertFalse(DemoOperationalSeed.complete(p,8));
        assertEquals(tasks,p.getObraTareas().size());assertEquals(logs,p.getRegistrosHoras().size());assertEquals(.123,first.getCantidadReal());
    }
    @Test void upgradesOnlyRecognizedEmptyLegacyTaskInPlace() {
        var p=fixture(); var t=new ObraTarea();t.setId(99L);t.setProyecto(p);t.setNombre("Corte de soga");
        t.setRubro("Hormigón armado");t.setUnidad("m2");t.setPorcentajeCumplimiento(94.);t.setComentario(DemoOperationalSeed.LEGACY_NOTE);p.getObraTareas().add(t);
        DemoOperationalSeed.complete(p,8);assertEquals(99L,t.getId());assertEquals(1L,t.getRubroId());assertNotNull(t.getFecha());
    }
    @Test void manualTaskBlocksAutomaticProductionForItsActivity() {
        var p=fixture();var t=new ObraTarea();t.setRubro("Hormigón armado");t.setCantidadReal(7.);p.getObraTareas().add(t);
        DemoOperationalSeed.complete(p,8);assertEquals(7.,t.getCantidadReal());
        assertEquals(1,p.getObraTareas().stream().filter(x -> "Hormigón armado".equals(x.getRubro())).count());
    }
    @Test void foreignCompanyAndUnknownProjectAreUntouched() {
        var p=fixture();p.setEmpresaRelacionada(new Empresa("Other","OTHER"));assertFalse(DemoOperationalSeed.complete(p,8));
        assertTrue(p.getObraTareas().isEmpty());p=fixture();p.setNumeroProyecto("USER-PROJECT");assertFalse(DemoOperationalSeed.complete(p,8));
    }
    @Test void hourSummariesEqualDailyInputsWithoutDoubleCounting() {
        var p=fixture();DemoOperationalSeed.complete(p,8);
        assertEquals(p.getObraTareas().stream().mapToDouble(t->t.getHorasOficiales()+t.getHorasAyudantes()).sum(),p.getHorasReales(),1e-7);
        assertEquals(p.getHorasReales(),p.getRegistrosHoras().stream().mapToDouble(RegistroHoras::getHoras).sum(),1e-7);
        assertEquals(p.getObraTareas().stream().mapToDouble(ObraTarea::getCantidadReal).sum(),p.getRegistrosHoras().stream().mapToDouble(RegistroHoras::getCantidadReal).sum(),1e-7);
        assertTrue(p.getRegistrosHoras().stream().allMatch(h->h.getFin().isAfter(h.getInicio())));
    }
    @Test void editedLegacyComplianceIsNotReplaced() {
        var p=fixture();var t=new ObraTarea();t.setNombre("Corte de soga");t.setRubro("Hormigón armado");
        t.setUnidad("m2");t.setPorcentajeCumplimiento(50.);t.setComentario(DemoOperationalSeed.LEGACY_NOTE);p.getObraTareas().add(t);
        DemoOperationalSeed.complete(p,8);assertEquals(50.,t.getPorcentajeCumplimiento());assertNull(t.getRubroId());
    }
    @Test void existingManualHourRecordsAreNotReplacedOrDuplicated() {
        var p=fixture();var record=new RegistroHoras();record.setNumeroRubro("5000");record.setRubro("Hormigón armado");record.setSubRubro("Fundación");
        record.setDescripcion("Carga manual");record.setCantidadReal(7.);record.setHoras(64.);p.getRegistrosHoras().add(record);
        DemoOperationalSeed.complete(p,8);assertEquals(7.,record.getCantidadReal());assertEquals(64.,record.getHoras());
        assertEquals(1,p.getRegistrosHoras().stream().filter(r->"5000".equals(r.getNumeroRubro())).count());
    }

}
