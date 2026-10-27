package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

// In-memory repository answers only: no Spring context or database.
@ExtendWith(MockitoExtension.class)
class DemoSeederCoverageTest {
    @Mock ProyectoRepository projects;
    @Mock PlantaRepository plants;
    @Mock EntregaRepository deliveries;
    @Mock TareaRepository tasks;
    @Mock RendimientoRepository yields;
    @Mock IngresoRepository incomes;
    @Mock GastoFijoRepository fixedCosts;
    @Mock PresupuestoLineaRepository budgets;
    @Mock EmpresaRepository companies;
    @Mock ResponsableEmpresaRepository contacts;
    @Mock UnidadMetricaEmpresaRepository units;
    @Mock CatalogoEmpresaRepository catalogs;
    @Mock CategoriaManoObraEmpresaRepository categories;
    @Mock CalendarioLaboralEmpresaRepository calendars;
    @Mock SeedRunPolicy seedRunPolicy;
    @InjectMocks DataSeeder seeder;
    Empresa company;
    List<Proyecto> ps=new ArrayList<>();
    List<CategoriaManoObraEmpresa> cs=new ArrayList<>();
    List<UnidadMetricaEmpresa> us=new ArrayList<>();
    List<CatalogoEmpresa> catalogData=new ArrayList<>();
    List<ResponsableEmpresa> contactData=new ArrayList<>();
    List<CalendarioLaboralEmpresa> calendarData=new ArrayList<>();
    List<PresupuestoLinea> bs=new ArrayList<>();
    List<Ingreso> ins=new ArrayList<>();
    List<GastoFijo> gs=new ArrayList<>();
    List<Rendimiento> ys=new ArrayList<>();
    @BeforeEach void repositories() {
        when(seedRunPolicy.shouldRunSeeders()).thenReturn(true);
        company=new Empresa("Renamed demo","DEMO-0001");ReflectionTestUtils.setField(company,"id",1L);
        when(companies.findByIdentificacionFiscal("DEMO-0001")).thenReturn(Optional.of(company));
        when(projects.findAll()).thenAnswer(i->ps);
        when(projects.save(any())).thenAnswer(i->{Proyecto p=i.getArgument(0); if(p.getId()==null){p.setId(1L);ps.add(p);}
            for(int n=0;n<p.getRubros().size();n++) if(p.getRubros().get(n).getId()==null)p.getRubros().get(n).setId((long)n+1); return p;});
        when(categories.findByEmpresaId(1L)).thenAnswer(i->cs);
        when(categories.save(any())).thenAnswer(i->{CategoriaManoObraEmpresa c=i.getArgument(0);if(!cs.contains(c))cs.add(c);return c;});
        when(units.findByEmpresaId(1L)).thenAnswer(i->us);
        when(units.save(any())).thenAnswer(i->{UnidadMetricaEmpresa u=i.getArgument(0);us.add(u);return u;});
        when(catalogs.findByEmpresaIdAndTipo(eq(1L),anyString())).thenAnswer(i->catalogData.stream().filter(c->c.getTipo().equals(i.getArgument(1))).toList());
        when(catalogs.save(any())).thenAnswer(i->{CatalogoEmpresa c=i.getArgument(0);catalogData.add(c);return c;});
        when(contacts.findByEmpresaId(1L)).thenAnswer(i->contactData);
        when(contacts.save(any())).thenAnswer(i->{ResponsableEmpresa c=i.getArgument(0);contactData.add(c);return c;});
        when(calendars.findByEmpresaId(1L)).thenAnswer(i->calendarData.stream().findFirst());
        when(calendars.save(any())).thenAnswer(i->{CalendarioLaboralEmpresa c=i.getArgument(0);calendarData.add(c);return c;});
        when(budgets.findByProyectoId(1L)).thenAnswer(i->bs);
        when(budgets.save(any())).thenAnswer(i->{PresupuestoLinea b=i.getArgument(0);bs.add(b);return b;});
        when(incomes.findByProyectoId(1L)).thenAnswer(i->ins);
        when(incomes.save(any())).thenAnswer(i->{Ingreso n=i.getArgument(0);ins.add(n);return n;});
        when(fixedCosts.findAll()).thenAnswer(i->gs);
        when(fixedCosts.save(any())).thenAnswer(i->{GastoFijo g=i.getArgument(0);gs.add(g);return g;});
        when(yields.findAll()).thenAnswer(i->ys);
        when(yields.save(any())).thenAnswer(i->{Rendimiento y=i.getArgument(0);ys.add(y);return y;});
    }
    @Test void fillsEveryDemoSectionAndDoesNotDuplicateOnRestart() {
        seeder.run();var p=ps.get(0);
        assertEquals(6,p.getRubros().size());assertEquals(4,p.getPlantas().size());assertEquals(5,p.getRestricciones().size());
        assertEquals(2,p.getEntregas().size());assertFalse(p.getObraTareas().isEmpty());assertFalse(p.getRegistrosHoras().isEmpty());
        assertEquals(12,us.size());assertEquals(3,cs.size());assertEquals(3,contactData.size());assertEquals(1,calendarData.size());
        assertEquals(7,catalogData.stream().filter(c->"CAUSA_INCUMPLIMIENTO".equals(c.getTipo())).count());
        assertEquals(6,bs.size());assertEquals(7,ins.size());assertEquals(5,gs.size());assertEquals(7,ys.size());
        double plannedCost=p.getRubros().stream().mapToDouble(r->r.getHorasOficialesPrevistas()*2000+r.getHorasAyudantesPrevistas()*1000).sum();
        assertEquals(plannedCost,bs.stream().mapToDouble(b->b.getM2()*b.getHhPorM2()*b.getPrecioPorHh()).sum(),1e-6);
        assertEquals(plannedCost*1.25,ins.stream().mapToDouble(Ingreso::getMonto).sum(),1e-6);
        int count=p.getObraTareas().size(),catalogCount=catalogData.size();
        seeder.run();
        assertEquals(1,ps.size());assertEquals(count,p.getObraTareas().size());assertEquals(catalogCount,catalogData.size());
        assertEquals(12,us.size());assertEquals(6,bs.size());assertEquals(7,ins.size());assertEquals(5,gs.size());
        assertTrue(p.getObraTareas().stream().allMatch(t->t.getRubroId()!=null));
    }
    @Test void preservesExistingTariffsUnitsAndEditedFixtureAmounts() {
        var official=new CategoriaManoObraEmpresa(company,"OFICIAL",3456.,true);cs.add(official);
        us.add(new UnidadMetricaEmpresa(company,"m3","Personalizado"));
        seeder.run();var p=ps.get(0);var load=p.getObraTareas().get(0);load.setCantidadReal(.5);
        var budget=bs.get(0);budget.setPrecioPorHh(999.);var income=ins.get(0);income.setMonto(123.);
        seeder.run();
        assertEquals(3456.,official.getCostoHora());assertEquals(12,us.size());assertEquals("Personalizado",us.get(0).getNombre());
        assertEquals(.5,load.getCantidadReal());assertEquals(999.,budget.getPrecioPorHh());assertEquals(123.,income.getMonto());
    }
    @Test void sameNameInForeignCompanyDoesNotPreventCreatingOwnedDemo() {
        var foreign=new Empresa("Foreign","OTHER");ReflectionTestUtils.setField(foreign,"id",9L);
        var other=new Proyecto();other.setId(9L);other.setNombre("PROYECTO DEMO CRONOGRAMA");other.setEmpresaRelacionada(foreign);ps.add(other);
        seeder.run();assertEquals(2,ps.size());assertTrue(other.getObraTareas().isEmpty());assertSame(foreign,other.getEmpresaRelacionada());
    }
}
