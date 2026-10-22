/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.stereotype.Component
 */
package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.domain.model.Entrega;
import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.Rendimiento;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.StatusTarea;
import com.vortise.gestion.domain.model.Tarea;
import com.vortise.gestion.domain.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.model.ResponsableEmpresa;
import com.vortise.gestion.domain.model.UnidadMetricaEmpresa;
import com.vortise.gestion.domain.repository.EntregaRepository;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PlantaRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RendimientoRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder
implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final ProyectoRepository proyectoRepository;
    private final PlantaRepository plantaRepository;
    private final EntregaRepository entregaRepository;
    private final TareaRepository tareaRepository;
    private final RendimientoRepository rendimientoRepository;
    private final IngresoRepository ingresoRepository;
    private final GastoFijoRepository gastoFijoRepository;
    private final PresupuestoLineaRepository presupuestoLineaRepository;
    private final ResponsableEmpresaRepository responsableEmpresaRepository;
    private final UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository;
    private final CatalogoEmpresaRepository catalogoEmpresaRepository;
    private final CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository;
    private final CalendarioLaboralEmpresaRepository calendarioLaboralEmpresaRepository;
    private final SeedRunPolicy seedRunPolicy;

    private final EmpresaRepository empresaRepository;

    @Override
    @Transactional
    public void run(String ... args) {
        if (!seedRunPolicy.shouldRunSeeders()) {
            log.info("Existing database detected; demo seed data is left unchanged.");
            return;
        }
        Empresa empresaDemo = ensureEmpresaSeedData();
        ensureFixedCosts();
        var demoProjects = this.proyectoRepository.findAll().stream()
            .filter(p -> p.getEmpresaRelacionada() != null && empresaDemo.getId().equals(p.getEmpresaRelacionada().getId()))
            .filter(p -> "P-2026-101".equals(p.getNumeroProyecto()) || "PROYECTO DEMO CRONOGRAMA".equals(p.getNombre())).toList();
        if (!demoProjects.isEmpty()) {
            for (var proyecto : demoProjects) {
                if (DemoOperationalSeed.complete(proyecto, demoJornada(empresaDemo))) this.proyectoRepository.save(proyecto);
                ensureFinancialSeedData(proyecto);
            }
            log.info("Demo project already seeded. Ensuring report seed data exists.");
            this.ensureRendimientoSeedData();
            return;
        }

        this.seedDemoProject();
        this.ensureRendimientoSeedData();
    }

    /** Backfill only identified demo fixtures; existing non-null amounts remain authoritative. */
    static boolean completeDemoMonthlyProgress(Proyecto proyecto) {
        if (!"PROYECTO DEMO CRONOGRAMA".equals(proyecto.getNombre())
                || !"P-2026-101".equals(proyecto.getNumeroProyecto())
                || proyecto.getEmpresaRelacionada() == null
                || !"DEMO-0001".equals(proyecto.getEmpresaRelacionada().getIdentificacionFiscal())) return false;
        String description = "Jornada real de obra: seguimiento de productividad y costo";
        String[] names = {"Hormigón armado", "Vaciado de losas", "Mampostería", "Pintura", "Instalaciones sanitarias", "Instalaciones eléctricas"};
        String[] subNames = {"Fundación", "Planta Baja", "Piso 1", "Piso 2", "Sanitarios", "Eléctricas"};
        double[] completion = {1.0, 0.85, 0.60, 0.40, 0.25, 0.10};
        boolean changed = false;
        LocalDate previousEnd = null;
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            RubroObra rubro = proyecto.getRubros().stream()
                .filter(r -> String.valueOf(5000 + index).equals(r.getnRubro())
                    && names[index].equals(r.getRubro()) && subNames[index].equals(r.getSubRubro()))
                .findFirst().orElse(null);
            if (rubro == null || rubro.getCantidad() == null || rubro.getCantidad() <= 0) continue;
            LocalDate plannedStart = rubro.getFechaInicioPlanificada() != null ? rubro.getFechaInicioPlanificada()
                : previousEnd != null ? previousEnd.plusDays(1) : proyecto.getFechaInicio();
            double plannedHours = (rubro.getHorasOficialesPrevistas() == null ? 0 : rubro.getHorasOficialesPrevistas())
                + (rubro.getHorasAyudantesPrevistas() == null ? 0 : rubro.getHorasAyudantesPrevistas());
            if (plannedHours == 0) plannedHours = rubro.getCantidad() * (rubro.getProductividad() == null ? 0 : rubro.getProductividad());
            long days = rubro.getCantidadPersonas() != null && rubro.getCantidadPersonas() > 0 && plannedHours > 0
                ? Math.max(1, (long) Math.ceil(plannedHours / (rubro.getCantidadPersonas() * 8.0))) : 1;
            LocalDate plannedEnd = rubro.isCronogramaManual() && rubro.getFechaFinPlanificada() != null
                ? rubro.getFechaFinPlanificada() : plannedStart == null ? null : plannedStart.plusDays(days - 1);
            previousEnd = plannedEnd;
            if (plannedStart == null || plannedEnd == null || plannedEnd.isBefore(plannedStart)) continue;
            List<RegistroHoras> matching = proyecto.getRegistrosHoras().stream()
                .filter(r -> java.util.Objects.equals(rubro.getnRubro(), r.getNumeroRubro())
                    && java.util.Objects.equals(rubro.getRubro(), r.getRubro())
                    && java.util.Objects.equals(rubro.getSubRubro(), r.getSubRubro())).toList();
            // Do not mix generated quantities with manually entered records.
            if (matching.stream().anyMatch(r -> !description.equals(r.getDescripcion()))) continue;
            if (matching.isEmpty()) {
                for (int j = 0; j < 4; j++) {
                    RegistroHoras record = new RegistroHoras();
                    record.setProyecto(proyecto);
                    record.setNumeroRubro(rubro.getnRubro());
                    record.setRubro(rubro.getRubro());
                    record.setSubRubro(rubro.getSubRubro());
                    record.setResponsable("Equipo demo");
                    record.setInicio(demoProgressStart(plannedStart, plannedEnd, i, j));
                    record.setFin(record.getInicio().plusHours(8));
                    record.setHoras(8.0);
                    record.setCantidadPrevista(rubro.getCantidad() / 4.0);
                    record.setCantidadReal(rubro.getCantidad() * completion[i] / 4.0);
                    record.setDescripcion(description);
                    proyecto.getRegistrosHoras().add(record);
                }
                changed = true;
            } else {
                // Migrate only complete four-record batches with known generated timestamps.
                // Edited timestamps and mixed/manual batches are not eligible.
                List<RegistroHoras> ordered = matching.stream()
                    .sorted(java.util.Comparator.comparing(RegistroHoras::getInicio,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))).toList();
                boolean generatedBatch = ordered.size() == 4;
                for (int j = 0; generatedBatch && j < 4; j++) {
                    generatedBatch = knownDemoTiming(ordered.get(j), i, j);
                }
                if (generatedBatch) {
                    for (int j = 0; j < 4; j++) {
                        RegistroHoras record = ordered.get(j);
                        java.time.Duration duration = java.time.Duration.between(record.getInicio(), record.getFin());
                        OffsetDateTime start = demoProgressStart(plannedStart, plannedEnd, i, j);
                        if (!start.equals(record.getInicio())) {
                            record.setInicio(start);
                            record.setFin(start.plus(duration));
                            changed = true;
                        }
                    }
                }
                List<RegistroHoras> missing = matching.stream()
                    .filter(r -> r.getCantidadReal() == null && r.getFin() != null).toList();
                double recorded = matching.stream().filter(r -> r.getCantidadReal() != null)
                    .mapToDouble(RegistroHoras::getCantidadReal).sum();
                double remaining = Math.max(0, rubro.getCantidad() * completion[i] - recorded);
                for (RegistroHoras record : missing) {
                    record.setCantidadReal(remaining / missing.size());
                    if (record.getCantidadPrevista() == null) record.setCantidadPrevista(rubro.getCantidad() / matching.size());
                    changed = true;
                }
            }
        }
        if (changed) proyecto.setHorasReales(proyecto.getRegistrosHoras().stream()
            .mapToDouble(r -> r.getHoras() == null ? 0 : r.getHoras()).sum());
        return changed;
    }

    /** Four synthetic progress checkpoints distributed across the plan, with small delays. */
    static OffsetDateTime demoProgressStart(LocalDate start, LocalDate end, int activity, int checkpoint) {
        long span = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        long offset = Math.round(span * (checkpoint + 1) / 4.0);
        // Foundation finishes on plan; later activities illustrate increasing delays.
        return start.plusDays(offset + activity * 3L).atTime(8, 0).atOffset(ZoneOffset.of("-03:00"));
    }

    static boolean knownDemoTiming(RegistroHoras record, int activity, int checkpoint) {
        if (record.getInicio() == null || record.getFin() == null) return false;
        OffsetDateTime base = OffsetDateTime.of(2026, 8, 3, 8, 0, 0, 0, ZoneOffset.of("-03:00"));
        OffsetDateTime legacy = base.plusDays(activity * 3L + checkpoint).plusHours((checkpoint % 3) * 2L);
        OffsetDateTime recent = base.plusMonths(activity).plusDays(checkpoint * 20L + activity * 5L)
            .plusHours((checkpoint % 3) * 2L);
        OffsetDateTime added = base.plusMonths(activity).plusDays(checkpoint * 20L);
        long hours = 6 + (checkpoint % 3) * 2L;
        return (legacy.equals(record.getInicio()) && legacy.plusHours(hours).equals(record.getFin()))
            || (recent.equals(record.getInicio()) && recent.plusHours(hours).equals(record.getFin()))
            || (added.equals(record.getInicio()) && added.plusHours(8).equals(record.getFin()));
    }

    @Transactional
    public void resetDemoData() {
        log.info("Resetting demo seed data...");
        this.ensureEmpresaSeedData();
        this.proyectoRepository.findAll()
            .stream()
            .filter(proyecto -> "PROYECTO DEMO CRONOGRAMA".equals(proyecto.getNombre()))
            .forEach(proyecto -> this.proyectoRepository.deleteById(proyecto.getId()));

        this.gastoFijoRepository.findAll().forEach(gasto -> this.gastoFijoRepository.deleteById(gasto.getId()));

        this.seedDemoProject();
        this.ensureRendimientoSeedData();
    }

    private Empresa ensureEmpresaSeedData() {
        Empresa empresa = this.empresaRepository.findByIdentificacionFiscal("DEMO-0001")
            .orElseGet(() -> {
                if (this.empresaRepository.findByNombreIgnoreCase("Constructora Norte").isPresent()) {
                    throw new IllegalStateException("El nombre de la empresa demo pertenece a otra identificación fiscal");
                }
                Empresa nueva = new Empresa("Constructora Norte", "DEMO-0001");
                nueva.setLimiteUsuarios(25);
                return this.empresaRepository.save(nueva);
            });
        this.seedEmpresaConfiguration(empresa);
        return empresa;
    }

    private void seedEmpresaConfiguration(Empresa empresa) {
        var responsables = responsableEmpresaRepository.findByEmpresaId(empresa.getId());
        String[][] contacts = {{"Director de obra", "Laura Torres", "laura.torres@constructora-norte.test", "+54 341 555-0101"},
            {"Jefe de obra", "Javier Ramos", "javier.ramos@constructora-norte.test", "+54 341 555-0102"},
            {"Compras", "Marina Sosa", "marina.sosa@constructora-norte.test", "+54 341 555-0103"}};
        for (var c : contacts) if (responsables.stream().noneMatch(r -> c[0].equalsIgnoreCase(r.getRol())))
            responsableEmpresaRepository.save(new ResponsableEmpresa(empresa,c[0],c[1],c[2],c[3]));
        var unidades = unidadMetricaEmpresaRepository.findByEmpresaId(empresa.getId());
        String[][] units = {{"ML","Metro lineal"},{"M2","Metro cuadrado"},{"M3","Metro cúbico"},{"GLOBAL","Global"},
            {"MES","Mes"},{"LTS","Litros"},{"TON","Tonelada"},{"KG","Kilogramo"},{"BOLSA","Bolsa"},
            {"PAQ","Paquete"},{"LATA","Lata"},{"UN","Unidad"}};
        for (var u : units) if (unidades.stream().noneMatch(r -> u[0].equalsIgnoreCase(r.getCodigo())))
            unidadMetricaEmpresaRepository.save(new UnidadMetricaEmpresa(empresa,u[0],u[1]));
        ensureCatalog(empresa,"SUBCONTRATISTA",new String[][]{{"SUB-HID","Hidrosur Instalaciones"},{"SUB-ELE","Electro Delta"}});
        ensureCatalog(empresa,"TIPO_RESTRICCION",new String[][]{{"DOC","Documentacion"},{"MAT","Materiales"},{"MO","Mano de obra"},{"EQ","Equipos"}});
        ensureCatalog(empresa,"CAUSA_INCUMPLIMIENTO",new String[][]{{"DOC-INC","Documentacion inconsistente"},
            {"MAT-FAL","Falta de materiales"},{"MO-FAL","Falta de mano de obra/subcontrato"},{"PLAN","Mala planificacion"},
            {"PROV","Falla del proveedor"},{"ADM","Problemas administrativos"},{"CLI","Clima"}});
        var categories = categoriaManoObraEmpresaRepository.findByEmpresaId(empresa.getId());
        String[] names = {"OFICIAL","AYUDANTE","CAPATAZ"};
        double[] costs = {2000.,1000.,2500.};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            var existing = categories.stream().filter(c -> name.equals(c.getNombre())).findFirst();
            if (existing.isEmpty()) categoriaManoObraEmpresaRepository.save(new CategoriaManoObraEmpresa(empresa,name,costs[i],true));
            else if (existing.get().getCostoHora() == null) {
                existing.get().setCostoHora(costs[i]);
                categoriaManoObraEmpresaRepository.save(existing.get());
            }
        }
        if (calendarioLaboralEmpresaRepository.findByEmpresaId(empresa.getId()).isEmpty()) {
            var calendar = new CalendarioLaboralEmpresa(empresa);
            calendar.setHorasLunesAViernes(8.0); calendar.setHorasSabado(0.0); calendar.setTrabajaDomingo(false);
            calendarioLaboralEmpresaRepository.save(calendar);
        }
    }

    private void ensureCatalog(Empresa empresa, String type, String[][] rows) {
        var existing = catalogoEmpresaRepository.findByEmpresaIdAndTipo(empresa.getId(),type);
        for (var row : rows) if (existing.stream().noneMatch(c -> row[0].equalsIgnoreCase(c.getCodigo())))
            catalogoEmpresaRepository.save(new CatalogoEmpresa(empresa,type,row[0],row[1]));
    }

    private double demoJornada(Empresa empresa) {
        return calendarioLaboralEmpresaRepository.findByEmpresaId(empresa.getId())
            .map(CalendarioLaboralEmpresa::getHorasLunesAViernes).filter(h -> h != null && Double.isFinite(h) && h > 0).orElse(8.0);
    }

    private void ensureFixedCosts() {
        var existing = gastoFijoRepository.findAll();
        for (Object[] row : new Object[][]{{"Alquiler oficina",350000.0,"ESTUDIO"},{"Sueldos staff",2000000.0,"ESTUDIO"},
                {"Servicios",180000.0,"ESTUDIO"},{"Otros",90000.0,"ESTUDIO"},{"Marketing",150000.0,"MARKETING"}}) {
            if (existing.stream().noneMatch(g -> row[0].equals(g.getConcepto())))
                gastoFijoRepository.save(GastoFijo.builder().concepto((String)row[0]).monto((Double)row[1]).categoria((String)row[2]).mes(null).build());
        }
    }

    private void ensureFinancialSeedData(Proyecto proyecto) {
        if (!DemoOperationalSeed.isDemo(proyecto) || proyecto.getRubros().isEmpty()) return;
        var categories = categoriaManoObraEmpresaRepository.findByEmpresaId(proyecto.getEmpresaRelacionada().getId());
        Double official = categories.stream().filter(c -> "OFICIAL".equals(c.getNombre()) && c.isHabilitada())
            .map(CategoriaManoObraEmpresa::getCostoHora).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        Double helper = categories.stream().filter(c -> "AYUDANTE".equals(c.getNombre()) && c.isHabilitada())
            .map(CategoriaManoObraEmpresa::getCostoHora).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (official == null || helper == null || !Double.isFinite(official) || !Double.isFinite(helper) || official < 0 || helper < 0) return;
        String prefix = "DEMO PVR · ";
        var budgets = presupuestoLineaRepository.findByProyectoId(proyecto.getId());
        // A user-created budget is authoritative; do not add a second simulated budget to it.
        boolean demoBudget = budgets.stream().allMatch(b -> b.getEtapa() != null && b.getEtapa().startsWith(prefix));
        if (demoBudget) for (var r : proyecto.getRubros()) {
            String key = prefix + r.getnRubro();
            if (budgets.stream().anyMatch(b -> key.equals(b.getEtapa()))) continue;
            double hoursOf = r.getHorasOficialesPrevistas() == null ? 0 : r.getHorasOficialesPrevistas();
            double hoursAy = r.getHorasAyudantesPrevistas() == null ? 0 : r.getHorasAyudantesPrevistas();
            double hours = hoursOf + hoursAy;
            double area = proyecto.getPlantas().stream().filter(p -> java.util.Objects.equals(p.getNivel(),r.getNivel()))
                .map(Planta::getSuperficie).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum();
            if (!Double.isFinite(hours) || hours <= 0 || !Double.isFinite(area) || area <= 0) continue;
            // Existing budget API is per m2: normalize the activity HH over its actual floor area.
            int month = proyecto.getFechaInicio() == null || r.getFechaInicioPlanificada() == null ? 1
                : (int)java.time.temporal.ChronoUnit.MONTHS.between(proyecto.getFechaInicio().withDayOfMonth(1),r.getFechaInicioPlanificada().withDayOfMonth(1))+1;
            presupuestoLineaRepository.save(PresupuestoLinea.builder().proyecto(proyecto).etapa(key)
                .tipoSuperficie("CUBIERTA").funcion(r.getSubRubro()).sistema(r.getRubro()).m2(area)
                .hhPorM2(hours/area).precioPorHh((hoursOf*official+hoursAy*helper)/hours)
                .mes(Math.max(1,month)).build());
        }
        var ingresos = ingresoRepository.findByProyectoId(proyecto.getId());
        if (!demoBudget || ingresos.stream().anyMatch(i -> i.getConcepto() == null || !i.getConcepto().startsWith(prefix))) return;
        // Monetary units match company hourly costs; never infer a USD conversion from presupuestoUsd.
        double labor = proyecto.getRubros().stream().mapToDouble(r ->
            (r.getHorasOficialesPrevistas()==null?0:r.getHorasOficialesPrevistas())*official
            +(r.getHorasAyudantesPrevistas()==null?0:r.getHorasAyudantesPrevistas())*helper).sum();
        if (!Double.isFinite(labor) || labor <= 0) return;
        int months = proyecto.getFechaInicio()==null || proyecto.getFechaFin()==null ? 7
            : (int)java.time.temporal.ChronoUnit.MONTHS.between(proyecto.getFechaInicio().withDayOfMonth(1),proyecto.getFechaFin().withDayOfMonth(1))+1;
        for (int month=1; month<=Math.max(1,months); month++) {
            String key = prefix+"Certificado mes "+month;
            if (ingresos.stream().noneMatch(i -> key.equals(i.getConcepto())))
                ingresoRepository.save(Ingreso.builder().proyecto(proyecto).concepto(key).mes(month)
                    .monto(labor*1.25/Math.max(1,months)).build());
        }
    }

    private void ensureRendimientoSeedData() {
        if (!this.rendimientoRepository.findAll().isEmpty()) {
            return;
        }

        log.info("Seeding rendimiento demo data...");
        List<Rendimiento> rows = List.of(
            Rendimiento.builder().fase("Fundación").etapa("Excavación").sistema("Hormigón armado").rol("Capataz").hhPorM2(6.5).precioPorHh(4800.0).build(),
            Rendimiento.builder().fase("Fundación").etapa("Vigas").sistema("Hormigón armado").rol("Cálculo").hhPorM2(5.8).precioPorHh(5200.0).build(),
            Rendimiento.builder().fase("Estructura").etapa("Losas").sistema("Vaciado de losas").rol("Supervisión").hhPorM2(7.2).precioPorHh(5100.0).build(),
            Rendimiento.builder().fase("Muros").etapa("Mampostería").sistema("Mampostería").rol("Mampostería").hhPorM2(4.4).precioPorHh(4600.0).build(),
            Rendimiento.builder().fase("Instalaciones").etapa("Sanitarios").sistema("Instalaciones sanitarias").rol("Plomería").hhPorM2(5.1).precioPorHh(4900.0).build(),
            Rendimiento.builder().fase("Instalaciones").etapa("Eléctricas").sistema("Instalaciones eléctricas").rol("Electricista").hhPorM2(4.9).precioPorHh(5000.0).build(),
            Rendimiento.builder().fase("Acabados").etapa("Pintura").sistema("Pintura").rol("Pintor").hhPorM2(3.8).precioPorHh(4700.0).build()
        );

        for (Rendimiento row : rows) {
            this.rendimientoRepository.save(row);
        }
    }

    private void seedDemoProject() {
        log.info("Seeding initial data...");

        Proyecto demoCronograma = Proyecto.builder()
            .nombre("PROYECTO DEMO CRONOGRAMA")
            .descripcion("Obra de prueba con cronograma completo y productividad real para análisis de obra")
            .presupuestoUsd(1500000.0)
            .build();
        demoCronograma.setDireccion("Calle Mitre 1250, Rosario");
        demoCronograma.setNumeroProyecto("P-2026-101");
        demoCronograma.setFechaInicio(LocalDate.of(2026, 8, 1));
        demoCronograma.setFechaFin(LocalDate.of(2027, 2, 28));
        demoCronograma.setResponsablesObra("Laura Torres, Javier Ramos");
        demoCronograma.setCategoriasManoObra("OFICIAL, AYUDANTE, CAPATAZ");
        demoCronograma.setCantidadSectores(4);
        demoCronograma.setEmpresaRelacionada(this.empresaRepository.findByIdentificacionFiscal("DEMO-0001")
            .orElseThrow(() -> new IllegalStateException("Empresa demo no encontrada")));
        demoCronograma.setPresupuestoUsd(2450000.0);
        demoCronograma.setHorasPrevistas(0.0);
        demoCronograma.setHorasReales(0.0);
        demoCronograma = this.proyectoRepository.save(demoCronograma);

        demoCronograma.setPlantas(new ArrayList<>());
        demoCronograma.getPlantas().add(Planta.builder().proyecto(demoCronograma).nivel("FUNDACION").superficie(420.0).supCubierta(420.0).supCalculada(420.0).build());
        demoCronograma.getPlantas().add(Planta.builder().proyecto(demoCronograma).nivel("PB").superficie(960.0).supCubierta(960.0).supCalculada(960.0).build());
        demoCronograma.getPlantas().add(Planta.builder().proyecto(demoCronograma).nivel("P1").superficie(960.0).supCubierta(960.0).supCalculada(960.0).build());
        demoCronograma.getPlantas().add(Planta.builder().proyecto(demoCronograma).nivel("P2").superficie(960.0).supCubierta(960.0).supCalculada(960.0).build());

        demoCronograma.setRubros(new ArrayList<>());
        String[] rubrosNombre = new String[]{"Hormigón armado", "Vaciado de losas", "Mampostería", "Pintura", "Instalaciones sanitarias", "Instalaciones eléctricas"};
        Integer[] rubrosNumero = new Integer[]{5000, 5001, 5002, 5003, 5004, 5005};
        String[] subRubros = new String[]{"Fundación", "Planta Baja", "Piso 1", "Piso 2", "Sanitarios", "Eléctricas"};
        Double[] cantidades = new Double[]{155.4, 118.0, 96.5, 430.0, 220.0, 180.0};
        String[] unidades = new String[]{"m3", "m3", "m3", "m2", "m2", "m2"};
        String[] nivelesRubros = new String[]{"FUNDACION", "PB", "P1", "P2", "PB", "P1"};
        Double[] hhOficiales = new Double[]{4000.0, 2700.0, 1400.0, 4000.0, 2600.0, 1900.0};
        Double[] hhAyudantes = new Double[]{1881.5, 1430.0, 969.25, 1762.0, 1184.0, 944.0};
        Integer[] personasPorRubro = new Integer[]{8, 6, 5, 10, 6, 5};
        List<CatalogoEmpresa> subcontratistas = this.catalogoEmpresaRepository.findByEmpresaIdAndTipo(demoCronograma.getEmpresaRelacionada().getId(), "SUBCONTRATISTA");
        for (int i = 0; i < rubrosNombre.length; ++i) {
            RubroObra rubro = new RubroObra();
            rubro.setProyecto(demoCronograma);
            rubro.setnRubro(String.valueOf(rubrosNumero[i]));
            rubro.setRubro(rubrosNombre[i]);
            rubro.setNivel(nivelesRubros[i]);
            rubro.setSubRubro(subRubros[i]);
            rubro.setCantidad(cantidades[i]);
            rubro.setUnidad(unidades[i]);
            rubro.setProductividad((hhOficiales[i] + hhAyudantes[i]) / cantidades[i]);
            rubro.setEmpresa(demoCronograma.getEmpresaRelacionada().getNombre());
            rubro.setTipoContratista(i >= 4 ? "SUBCONTRATO" : "PROPIA");
            if (i >= 4 && !subcontratistas.isEmpty()) rubro.setSubcontratista(subcontratistas.get((i - 4) % subcontratistas.size()));
            rubro.setHorasOficialesPrevistas(hhOficiales[i]);
            rubro.setHorasAyudantesPrevistas(hhAyudantes[i]);
            rubro.setCantidadPersonas(personasPorRubro[i]);
            // Two calendar months per activity, for planned/actual comparison.
            LocalDate inicioPrevisto = LocalDate.of(2026, 8, 1).plusMonths(i);
            rubro.setFechaInicioPlanificada(inicioPrevisto);
            rubro.setFechaFinPlanificada(inicioPrevisto.plusMonths(2).minusDays(1));
            rubro.setCronogramaManual(true);
            demoCronograma.getRubros().add(rubro);
        }

        double[] horasPlanificadasPorRubro = new double[]{5881.5, 4130.0, 2369.25, 5762.0, 3784.0, 2844.0};
        double horasPrevistasTotales = 0.0;
        for (int i = 0; i < horasPlanificadasPorRubro.length; ++i) {
            horasPrevistasTotales += horasPlanificadasPorRubro[i];
        }
        demoCronograma.setHorasPrevistas(horasPrevistasTotales);

        demoCronograma.setEntregas(new ArrayList<>());
        demoCronograma.setObraTareas(new ArrayList<>());
        demoCronograma.setRestricciones(new ArrayList<>());

        Entrega entregaEstructura = new Entrega();
        entregaEstructura.setProyecto(demoCronograma);
        entregaEstructura.setNombre("Estructura y cerramientos");
        entregaEstructura.setOrden(1);
        entregaEstructura.setTareas(new ArrayList<>());

        String[][] tareasBase = new String[][]{{"Fundación", "Base", "Hormigón armado", "EN_PROCESO", "85", "1800000.0", "2026-08-01", "2026-09-15"}, {"Vigas", "Estructura", "Hormigón armado", "EJECUTADO", "92", "2200000.0", "2026-08-10", "2026-09-25"}, {"Losas", "Estructura", "Vaciado de losas", "EN_PROCESO", "78", "1750000.0", "2026-09-01", "2026-10-20"}, {"Mampostería", "Muros", "Mampostería", "PENDIENTE", "52", "980000.0", "2026-10-05", "2026-11-18"}, {"Pintura", "Acabado", "Pintura", "PENDIENTE", "38", "760000.0", "2026-11-02", "2026-12-20"}};
        for (String[] tareaData : tareasBase) {
            Tarea tarea = new Tarea();
            tarea.setEntrega(entregaEstructura);
            tarea.setFase(tareaData[0]);
            tarea.setEtapa(tareaData[1]);
            tarea.setSistema(tareaData[2]);
            tarea.setNombre(tareaData[0] + " - " + tareaData[1]);
            tarea.setStatus(StatusTarea.valueOf(tareaData[3]));
            tarea.setAvancePct(Integer.parseInt(tareaData[4]));
            tarea.setCantidad(Double.parseDouble(tareaData[5]) / 10000.0);
            tarea.setCosto(Double.parseDouble(tareaData[5]));
            tarea.setFechaInicio(LocalDate.parse(tareaData[6]));
            tarea.setFechaFin(LocalDate.parse(tareaData[7]));
            tarea.setMes(Integer.parseInt(tareaData[6].substring(5, 7)));
            entregaEstructura.getTareas().add(tarea);
        }

        Entrega entregaInstalaciones = new Entrega();
        entregaInstalaciones.setProyecto(demoCronograma);
        entregaInstalaciones.setNombre("Instalaciones y acabados");
        entregaInstalaciones.setOrden(2);
        entregaInstalaciones.setTareas(new ArrayList<>());

        String[][] tareasInstalaciones = new String[][]{{"Sanitarios", "Instalaciones", "Instalaciones sanitarias", "EN_PROCESO", "69", "1350000.0", "2026-08-15", "2026-10-15"}, {"Eléctricas", "Instalaciones", "Instalaciones eléctricas", "PENDIENTE", "44", "1200000.0", "2026-09-10", "2026-11-25"}, {"Pisos", "Acabados", "Pintura", "PENDIENTE", "34", "830000.0", "2026-10-12", "2026-12-05"}};
        for (String[] tareaData : tareasInstalaciones) {
            Tarea tarea = new Tarea();
            tarea.setEntrega(entregaInstalaciones);
            tarea.setFase(tareaData[0]);
            tarea.setEtapa(tareaData[1]);
            tarea.setSistema(tareaData[2]);
            tarea.setNombre(tareaData[0] + " - " + tareaData[1]);
            tarea.setStatus(StatusTarea.valueOf(tareaData[3]));
            tarea.setAvancePct(Integer.parseInt(tareaData[4]));
            tarea.setCantidad(Double.parseDouble(tareaData[5]) / 12000.0);
            tarea.setCosto(Double.parseDouble(tareaData[5]));
            tarea.setFechaInicio(LocalDate.parse(tareaData[6]));
            tarea.setFechaFin(LocalDate.parse(tareaData[7]));
            tarea.setMes(Integer.parseInt(tareaData[6].substring(5, 7)));
            entregaInstalaciones.getTareas().add(tarea);
        }

        demoCronograma.getEntregas().add(entregaEstructura);
        demoCronograma.getEntregas().add(entregaInstalaciones);

        String[] restriccionesTexto = new String[]{"Retiro de caja de escaleras", "Pedido de acero para vigas", "Hidrosanitario nivel P1", "Acondicionamiento de cerramiento", "Aprobación de pintura exterior"};
        String[] tipos = new String[]{"Equipos", "Materiales", "Documentacion", "Mano de obra", "Documentacion"};
        String[] sectoresRestriccion = new String[]{"Fundación", "Estructura", "P1", "PB", "Fachada"};
        String[] prioridad = new String[]{"ALTA", "MEDIA", "ALTA", "MEDIA", "BAJA"};
        String[] estado = new String[]{"RESUELTO", "RESUELTO", "PENDIENTE", "PENDIENTE", "PENDIENTE"};
        for (int i = 0; i < restriccionesTexto.length; ++i) {
            RestriccionObra restriccion = new RestriccionObra();
            restriccion.setProyecto(demoCronograma);
            restriccion.setRestriccion(restriccionesTexto[i]);
            restriccion.setTipo(tipos[i]);
            restriccion.setSector(sectoresRestriccion[i]);
            restriccion.setNivel(demoCronograma.getRubros().get(i).getNivel());
            restriccion.setUnidad(demoCronograma.getRubros().get(i).getUnidad());
            restriccion.setRubro(rubrosNombre[i % rubrosNombre.length]);
            restriccion.setResponsable(i % 2 == 0 ? "Laura Torres" : "Marina Sosa");
            LocalDate requested = demoCronograma.getRubros().get(i).getFechaInicioPlanificada();
            restriccion.setFechaSolicitud(requested);
            restriccion.setFechaMaximaEntrega(requested.plusDays(7));
            restriccion.setFechaRealEntrega(i < 2 ? requested.plusDays(5) : null);
            restriccion.setCarga(String.valueOf(12 + i * 3));
            restriccion.setPrioridad(prioridad[i]);
            restriccion.setEstado(estado[i]);
            restriccion.setOrden(i + 1);
            demoCronograma.getRestricciones().add(restriccion);
        }

        // Persist activities before linking newly created daily loads to them.
        demoCronograma = this.proyectoRepository.save(demoCronograma);
        DemoOperationalSeed.complete(demoCronograma, demoJornada(demoCronograma.getEmpresaRelacionada()));

        this.proyectoRepository.save(demoCronograma);
        ensureFinancialSeedData(demoCronograma);
        log.info("Demo operational and report fixtures ensured.");
    }

    @Autowired
    public DataSeeder(ProyectoRepository proyectoRepository, PlantaRepository plantaRepository, EntregaRepository entregaRepository, TareaRepository tareaRepository, RendimientoRepository rendimientoRepository, IngresoRepository ingresoRepository, GastoFijoRepository gastoFijoRepository, PresupuestoLineaRepository presupuestoLineaRepository, EmpresaRepository empresaRepository, ResponsableEmpresaRepository responsableEmpresaRepository, UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository, CatalogoEmpresaRepository catalogoEmpresaRepository, CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository, CalendarioLaboralEmpresaRepository calendarioLaboralEmpresaRepository, SeedRunPolicy seedRunPolicy) {
        this.proyectoRepository = proyectoRepository;
        this.plantaRepository = plantaRepository;
        this.entregaRepository = entregaRepository;
        this.tareaRepository = tareaRepository;
        this.rendimientoRepository = rendimientoRepository;
        this.ingresoRepository = ingresoRepository;
        this.gastoFijoRepository = gastoFijoRepository;
        this.presupuestoLineaRepository = presupuestoLineaRepository;
        this.empresaRepository = empresaRepository;
        this.responsableEmpresaRepository = responsableEmpresaRepository;
        this.unidadMetricaEmpresaRepository = unidadMetricaEmpresaRepository;
        this.catalogoEmpresaRepository = catalogoEmpresaRepository;
        this.categoriaManoObraEmpresaRepository = categoriaManoObraEmpresaRepository;
        this.calendarioLaboralEmpresaRepository = calendarioLaboralEmpresaRepository;
        this.seedRunPolicy = seedRunPolicy;
    }

    public DataSeeder(ProyectoRepository proyectoRepository, PlantaRepository plantaRepository, EntregaRepository entregaRepository, TareaRepository tareaRepository, RendimientoRepository rendimientoRepository, IngresoRepository ingresoRepository, GastoFijoRepository gastoFijoRepository, PresupuestoLineaRepository presupuestoLineaRepository, EmpresaRepository empresaRepository, ResponsableEmpresaRepository responsableEmpresaRepository, UnidadMetricaEmpresaRepository unidadMetricaEmpresaRepository, CatalogoEmpresaRepository catalogoEmpresaRepository, CategoriaManoObraEmpresaRepository categoriaManoObraEmpresaRepository, CalendarioLaboralEmpresaRepository calendarioLaboralEmpresaRepository) {
        this(proyectoRepository, plantaRepository, entregaRepository, tareaRepository, rendimientoRepository, ingresoRepository, gastoFijoRepository, presupuestoLineaRepository, empresaRepository, responsableEmpresaRepository, unidadMetricaEmpresaRepository, catalogoEmpresaRepository, categoriaManoObraEmpresaRepository, calendarioLaboralEmpresaRepository, () -> true);
    }
}
