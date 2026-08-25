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
import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.PresupuestoLinea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.Rendimiento;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.StatusTarea;
import com.vortise.gestion.domain.model.Tarea;
import com.vortise.gestion.domain.repository.EntregaRepository;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PlantaRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RendimientoRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    public void run(String ... args) {
        boolean demoProjectExists = this.proyectoRepository.findAll()
            .stream()
            .anyMatch(proyecto -> "PROYECTO DEMO CRONOGRAMA".equals(proyecto.getNombre()));

        if (demoProjectExists) {
            log.info("Demo project already seeded. Ensuring report seed data exists.");
            this.ensureRendimientoSeedData();
            return;
        }

        this.seedDemoProject();
        this.ensureRendimientoSeedData();
    }

    @Transactional
    public void resetDemoData() {
        log.info("Resetting demo seed data...");

        this.proyectoRepository.findAll()
            .stream()
            .filter(proyecto -> "PROYECTO DEMO CRONOGRAMA".equals(proyecto.getNombre()))
            .forEach(proyecto -> this.proyectoRepository.deleteById(proyecto.getId()));

        this.gastoFijoRepository.findAll().forEach(gasto -> this.gastoFijoRepository.deleteById(gasto.getId()));

        this.seedDemoProject();
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

        for (Object[] row : new Object[][]{{"Alquiler oficina", 350000.0, "ESTUDIO"}, {"Sueldos staff", 2000000.0, "ESTUDIO"}, {"Servicios", 180000.0, "ESTUDIO"}, {"Otros", 90000.0, "ESTUDIO"}, {"Marketing", 150000.0, "MARKETING"}}) {
            this.gastoFijoRepository.save(GastoFijo.builder().concepto((String)row[0]).monto((Double)row[1]).categoria((String)row[2]).mes(null).build());
        }

        Proyecto demoCronograma = Proyecto.builder()
            .nombre("PROYECTO DEMO CRONOGRAMA")
            .descripcion("Obra de prueba con cronograma completo y productividad real para análisis de obra")
            .presupuestoUsd(1500000.0)
            .build();
        demoCronograma.setDireccion("Calle Mitre 1250, Rosario");
        demoCronograma.setNumeroProyecto("P-2026-101");
        demoCronograma.setEmpresa("Constructora Norte");
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
        Double[] cantidades = new Double[]{145.0, 118.0, 96.5, 430.0, 220.0, 180.0};
        String[] unidades = new String[]{"m3", "m3", "m3", "m2", "m2", "m2"};
        Double[] productividades = new Double[]{38.5, 35.0, 24.5, 13.4, 17.2, 15.8};
        for (int i = 0; i < rubrosNombre.length; ++i) {
            RubroObra rubro = new RubroObra();
            rubro.setProyecto(demoCronograma);
            rubro.setnRubro(rubrosNumero[i]);
            rubro.setRubro(rubrosNombre[i]);
            rubro.setSubRubro(subRubros[i]);
            rubro.setCantidad(cantidades[i]);
            rubro.setUnidad(unidades[i]);
            rubro.setProductividad(productividades[i]);
            rubro.setEmpresa("Constructora Norte");
            demoCronograma.getRubros().add(rubro);
        }

        double[] horasPlanificadasPorRubro = new double[]{5582.5, 4130.0, 2369.25, 5762.0, 3784.0, 2844.0};
        double[] horasRealesPorRubro = new double[]{6430.0, 4890.0, 2890.0, 6960.0, 4560.0, 3320.0};
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

        String[] tareasObra = new String[]{"Corte de soga", "Apertura de accesos", "Replanteo de eje", "Preparación de encofrado", "Verificación de plomada", "Prueba de presión"};
        String[] sectores = new String[]{"Fundación", "Planta baja", "Piso 1", "Piso 2", "Instalaciones", "Acabado"};
        String[] niveles = new String[]{"N-1", "PB", "P1", "P2", "Sanitarios", "Cocina"};
        Double[] cumplimiento = new Double[]{94.0, 88.0, 91.0, 76.0, 68.0, 58.0};
        for (int i = 0; i < tareasObra.length; ++i) {
            ObraTarea obraTarea = new ObraTarea();
            obraTarea.setProyecto(demoCronograma);
            obraTarea.setNombre(tareasObra[i]);
            obraTarea.setSector(sectores[i]);
            obraTarea.setNivel(niveles[i]);
            obraTarea.setUnidad("m2");
            obraTarea.setRubro(rubrosNombre[i % rubrosNombre.length]);
            obraTarea.setPorcentajeCumplimiento(cumplimiento[i]);
            obraTarea.setCumplimiento(cumplimiento[i] >= 80.0 ? "OK" : "ATENCION");
            obraTarea.setCausaNoCumplimiento(cumplimiento[i] < 80.0 ? "Demora en entrega de materiales y clima" : "Sin incidencia");
            obraTarea.setComentario("Seguimiento de avance para control de obra");
            demoCronograma.getObraTareas().add(obraTarea);
        }

        String[] restriccionesTexto = new String[]{"Retiro de caja de escaleras", "Pedido de acero para vigas", "Hidrosanitario nivel P1", "Acondicionamiento de cerramiento", "Aprobación de pintura exterior"};
        String[] tipos = new String[]{"Logística", "Materiales", "Instalación", "Ejecutivo", "Calidad"};
        String[] sectoresRestriccion = new String[]{"Fundación", "Estructura", "P1", "PB", "Fachada"};
        String[] prioridad = new String[]{"ALTA", "MEDIA", "ALTA", "MEDIA", "BAJA"};
        String[] estado = new String[]{"PENDIENTE", "EN_PROCESO", "PENDIENTE", "EN_PROCESO", "RESUELTO"};
        for (int i = 0; i < restriccionesTexto.length; ++i) {
            RestriccionObra restriccion = new RestriccionObra();
            restriccion.setProyecto(demoCronograma);
            restriccion.setRestriccion(restriccionesTexto[i]);
            restriccion.setTipo(tipos[i]);
            restriccion.setSector(sectoresRestriccion[i]);
            restriccion.setNivel(i % 2 == 0 ? "N-1" : "P1");
            restriccion.setUnidad("u");
            restriccion.setRubro(rubrosNombre[i % rubrosNombre.length]);
            restriccion.setResponsable(i % 2 == 0 ? "Supervisión" : "Compras");
            restriccion.setFechaSolicitud(LocalDate.of(2026, 8, 5 + i));
            restriccion.setFechaMaximaEntrega(LocalDate.of(2026, 8, 15 + i * 4));
            restriccion.setFechaRealEntrega(i == 4 ? LocalDate.of(2026, 8, 20) : null);
            restriccion.setCarga(String.valueOf(12 + i * 3));
            restriccion.setPrioridad(prioridad[i]);
            restriccion.setEstado(estado[i]);
            restriccion.setOrden(i + 1);
            demoCronograma.getRestricciones().add(restriccion);
        }

        demoCronograma.setRegistrosHoras(new ArrayList<>());
        OffsetDateTime base = OffsetDateTime.of(2026, 8, 3, 8, 0, 0, 0, ZoneOffset.of("-03:00"));
        String[] responsables = new String[]{"L. Torres", "J. Ramos", "M. Sosa", "V. Díaz", "A. Costa", "R. Silva", "N. Cruz"};
        for (int i = 0; i < rubrosNombre.length; ++i) {
            for (int j = 0; j < 4; ++j) {
                RegistroHoras registro = new RegistroHoras();
                registro.setProyecto(demoCronograma);
                registro.setNumeroRubro(rubrosNumero[i]);
                registro.setRubro(rubrosNombre[i]);
                registro.setSubRubro(subRubros[i]);
                registro.setResponsable(responsables[(i + j) % responsables.length]);
                OffsetDateTime inicio = base.plusDays((long)i * 3L + (long)j).plusHours((j % 3) * 2L);
                OffsetDateTime fin = inicio.plusHours(6 + (j % 3) * 2L);
                double horas = horasRealesPorRubro[i] / 4.0;
                registro.setInicio(inicio);
                registro.setFin(fin);
                registro.setHoras(horas);
                registro.setDescripcion("Jornada real de obra: seguimiento de productividad y costo");
                demoCronograma.getRegistrosHoras().add(registro);
            }
        }

        demoCronograma.setHorasReales(demoCronograma.getRegistrosHoras().stream().mapToDouble(r -> r.getHoras() == null ? 0.0 : r.getHoras()).sum());

        this.proyectoRepository.save(demoCronograma);
        log.info("Data seeded successfully with realistic productivity, projected loss, and hour logs.");
    }

    public DataSeeder(ProyectoRepository proyectoRepository, PlantaRepository plantaRepository, EntregaRepository entregaRepository, TareaRepository tareaRepository, RendimientoRepository rendimientoRepository, IngresoRepository ingresoRepository, GastoFijoRepository gastoFijoRepository, PresupuestoLineaRepository presupuestoLineaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.plantaRepository = plantaRepository;
        this.entregaRepository = entregaRepository;
        this.tareaRepository = tareaRepository;
        this.rendimientoRepository = rendimientoRepository;
        this.ingresoRepository = ingresoRepository;
        this.gastoFijoRepository = gastoFijoRepository;
        this.presupuestoLineaRepository = presupuestoLineaRepository;
    }
}
