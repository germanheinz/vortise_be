package com.vortise.gestion.infrastructure.config;

import com.vortise.gestion.application.usecase.PrevistoRealCalculator;
import com.vortise.gestion.domain.model.*;
import java.time.*;
import java.util.*;

/** Synthetic project snapshot. Pure object fixture builder, no database access. */
final class DemoOperationalSeed {
    static final String MARKER = "DEMO-PVR-V1:";
    static final String LEGACY_NOTE = "Seguimiento de avance para control de obra";
    private static final String[] LEGACY_NAMES = {"Corte de soga","Apertura de accesos","Replanteo de eje",
        "Preparación de encofrado","Verificación de plomada","Prueba de presión"};
    private static final String[] RUBROS = {"Hormigón armado","Vaciado de losas","Mampostería","Pintura",
        "Instalaciones sanitarias","Instalaciones eléctricas"};
    private static final double[] COMPLETION = {1.,.85,.60,.40,.25,.10};
    private static final double[] HOURS_FACTOR = {.80,.95,1.05,1.25,1.10,.88};
    private DemoOperationalSeed() { }

    static boolean isDemo(Proyecto p) {
        return "PROYECTO DEMO CRONOGRAMA".equals(p.getNombre()) && "P-2026-101".equals(p.getNumeroProyecto())
            && p.getEmpresaRelacionada() != null && "DEMO-0001".equals(p.getEmpresaRelacionada().getIdentificacionFiscal());
    }

    static boolean complete(Proyecto p, double dailyHours) {
        if (!isDemo(p) || !Double.isFinite(dailyHours) || dailyHours <= 0 || p.getFechaFin() == null) return false;
        boolean changed = false;
        for (int i = 0; i < RUBROS.length; i++) {
            final int index = i;
            var candidates = p.getRubros().stream().filter(r -> String.valueOf(5000 + index).equals(r.getnRubro())
                && RUBROS[index].equals(r.getRubro())).toList();
            if (candidates.size() != 1) continue;
            var r = candidates.get(0);
            changed |= fillMissingActivity(r,p,i);
            if (!positive(r.getCantidad()) || !valid(r.getHorasOficialesPrevistas()) || !valid(r.getHorasAyudantesPrevistas())
                    || r.getCantidadPersonas() == null || r.getCantidadPersonas() <= 0
                    || r.getFechaInicioPlanificada() == null || r.getFechaFinPlanificada() == null) continue;
            String prefix = MARKER + r.getnRubro() + ":";
            var legacy = p.getObraTareas().stream().filter(t -> LEGACY_NAMES[index].equals(t.getNombre())
                && RUBROS[index].equals(t.getRubro()) && LEGACY_NOTE.equals(t.getComentario()) && "m2".equals(t.getUnidad())
                && Objects.equals(new Double[]{94.,88.,91.,76.,68.,58.}[index],t.getPorcentajeCumplimiento())
                && t.getRubroId() == null
                && t.getFecha() == null && t.getCantidadReal() == null && t.getHorasOficiales() == null && t.getHorasAyudantes() == null).toList();
            if (p.getObraTareas().stream().anyMatch(t -> t.getDescripcion() != null && t.getDescripcion().startsWith(prefix)
                    || r.getId() != null && r.getId().equals(t.getRubroId()))) {
                // These empty old demo placeholders are superseded by the activity's real loads.
                // Do not turn them into additional production or remove unrecognized/manual entries.
                changed |= p.getObraTareas().removeAll(legacy);
                continue;
            }
            // A manual/unrecognized load for this activity is authoritative; do not fabricate additional production.
            if (p.getObraTareas().stream().anyMatch(t -> RUBROS[index].equals(t.getRubro()) && !legacy.contains(t))) continue;
            double plannedHours = r.getHorasOficialesPrevistas() + r.getHorasAyudantesPrevistas();
            if (!positive(plannedHours)) continue;
            double hours = plannedHours * COMPLETION[i] * HOURS_FACTOR[i];
            double capacity = r.getCantidadPersonas() * dailyHours;
            int daysNeeded = (int) Math.ceil(hours / capacity);
            if (daysNeeded < 1 || daysNeeded > 3660) continue;
            LocalDate start = nextWorkingDay(r.getFechaInicioPlanificada().plusDays(i * 2L));
            LocalDate earliestFinish = start;
            for (int d = 1; d < daysNeeded; d++) earliestFinish = nextWorkingDay(earliestFinish.plusDays(1));
            LocalDate end = earliestFinish.isAfter(r.getFechaFinPlanificada()) ? earliestFinish : r.getFechaFinPlanificada();
            if (end.isAfter(p.getFechaFin())) end = p.getFechaFin();
            if (start.isAfter(end)) continue;
            var available = workingDays(start,end);
            int count = Math.min(daysNeeded,available.size());
            if (count == 0) continue;
            // If an edited plan leaves too little time, cap production to the available crew capacity.
            double actualHours = Math.min(hours,count * capacity);
            double quantity = actualHours / (plannedHours / r.getCantidad() * HOURS_FACTOR[i]);
            var generated = new ArrayList<ObraTarea>();
            for (int d = 0; d < count; d++) {
                var task = d < legacy.size() ? legacy.get(d) : new ObraTarea();
                task.setProyecto(p); task.setActividad(r);
                task.setNombre(r.getSubRubro() + " · jornada " + (d + 1));
                task.setRubro(r.getRubro()); task.setNivel(r.getNivel()); task.setUnidad(r.getUnidad());
                task.setSector("Sector " + (i % 4 + 1));
                task.setFecha(available.get(count == 1 ? 0 : (int)Math.round((double)d * (available.size()-1)/(count-1))));
                task.setCantidadReal(quantity/count);
                task.setHorasOficiales(actualHours/count * r.getHorasOficialesPrevistas()/plannedHours);
                task.setHorasAyudantes(actualHours/count * r.getHorasAyudantesPrevistas()/plannedHours);
                var metrics = PrevistoRealCalculator.calculate(r.getCantidad(),r.getHorasOficialesPrevistas(),r.getHorasAyudantesPrevistas(),
                    task.getCantidadReal(),task.getHorasOficiales(),task.getHorasAyudantes(),null,null,dailyHours);
                task.setCantidadPrevista(metrics.cantidadEquivalente());
                task.setPorcentajeCumplimiento(Math.round(task.getCantidadReal()/metrics.cantidadEquivalente()*10000.)/100.);
                boolean meets = HOURS_FACTOR[i] <= 1;
                task.setCumplimiento(meets ? "SI" : "NO");
                task.setCausaNoCumplimiento(meets ? null : "Falta de materiales");
                task.setMedidaCorrectiva(meets ? "Mantener la secuencia de trabajo" : "Confirmar el acopio antes de iniciar la jornada");
                task.setComentario("Datos simulados de cuadrilla; corte al " + p.getFechaFin());
                task.setDescripcion(prefix + d);
                if (!legacy.contains(task)) p.getObraTareas().add(task);
                generated.add(task);
            }
            completeHourSummaries(p,r,generated,i);
            changed = true;
        }
        changed |= completeRestrictions(p);
        if (changed) {
            p.setHorasPrevistas(p.getRubros().stream().mapToDouble(r -> safe(r.getHorasOficialesPrevistas())+safe(r.getHorasAyudantesPrevistas())).sum());
            p.setHorasReales(p.getObraTareas().stream().mapToDouble(t -> safe(t.getHorasOficiales())+safe(t.getHorasAyudantes())).sum());
            completeDeliveryProgress(p);
        }
        return changed;
    }

    private static boolean fillMissingActivity(RubroObra r, Proyecto p, int index) {
        boolean changed = false;
        if (r.getFechaInicioPlanificada()==null && r.getFechaFinPlanificada()==null && p.getFechaInicio()!=null) {
            r.setFechaInicioPlanificada(p.getFechaInicio().withDayOfMonth(1).plusMonths(index));
            r.setFechaFinPlanificada(r.getFechaInicioPlanificada().plusMonths(2).minusDays(1));
            r.setCronogramaManual(true); changed=true;
        }
        String[] levels={"FUNDACION","PB","P1","P2","PB","P1"};
        String[] units={"m3","m3","m3","m2","m2","m2"};
        String[] names={"Fundación","Planta Baja","Piso 1","Piso 2","Sanitarios","Eléctricas"};
        if(r.getNivel()==null){r.setNivel(levels[index]);changed=true;}
        if(r.getUnidad()==null){r.setUnidad(units[index]);changed=true;}
        if(r.getSubRubro()==null){r.setSubRubro(names[index]);changed=true;}
        if(r.getCantidadPersonas()==null){r.setCantidadPersonas(new int[]{8,6,5,10,6,5}[index]);changed=true;}
        return changed;
    }

    private static void completeHourSummaries(Proyecto p, RubroObra r, List<ObraTarea> tasks, int index) {
        var existing = p.getRegistrosHoras().stream().filter(h -> Objects.equals(r.getnRubro(),h.getNumeroRubro())
            && Objects.equals(r.getRubro(),h.getRubro()) && Objects.equals(r.getSubRubro(),h.getSubRubro())).toList();
        double[] oldTotals = {6430,4890,2890,6960,4560,3320};
        // Only replace recognized untouched four-row legacy fixtures. Never delete or overwrite manual logs.
        if (!existing.isEmpty() && (existing.size() != 4 || existing.stream().anyMatch(h ->
                !"Jornada real de obra: seguimiento de productividad y costo".equals(h.getDescripcion())
                || h.getHoras() == null || Math.abs(h.getHoras()-oldTotals[index]/4.) > .001
                || h.getCantidadReal() == null || Math.abs(h.getCantidadReal()-r.getCantidad()*COMPLETION[index]/4.) > .001))) return;
        if (!existing.isEmpty()) {
            var ordered = existing.stream().sorted(Comparator.comparing(RegistroHoras::getInicio,Comparator.nullsLast(Comparator.naturalOrder()))).toList();
            for (int j=0;j<ordered.size();j++) {
                var h=ordered.get(j);
                var expected=DataSeeder.demoProgressStart(r.getFechaInicioPlanificada(),r.getFechaFinPlanificada(),index,j);
                boolean known=expected.equals(h.getInicio()) && expected.plusHours(6+(j%3)*2L).equals(h.getFin());
                if (!known && !DataSeeder.knownDemoTiming(h,index,j)) return;
            }
        }
        int groups = Math.min(4,tasks.size());
        if (!existing.isEmpty() && groups != 4) return;
        for (int g = 0; g < groups; g++) {
            var group = tasks.subList(g*tasks.size()/groups,(g+1)*tasks.size()/groups);
            var h = existing.isEmpty() ? new RegistroHoras() : existing.get(g);
            h.setProyecto(p); h.setNumeroRubro(r.getnRubro()); h.setRubro(r.getRubro()); h.setSubRubro(r.getSubRubro());
            h.setResponsable("Javier Ramos"); h.setEtapa("Seguimiento de cuadrilla");
            h.setInicio(group.get(0).getFecha().atTime(8,0).atOffset(ZoneOffset.ofHours(-3)));
            h.setFin(group.get(group.size()-1).getFecha().atTime(16,0).atOffset(ZoneOffset.ofHours(-3)));
            h.setHorasOficiales(group.stream().mapToDouble(ObraTarea::getHorasOficiales).sum());
            h.setHorasAyudantes(group.stream().mapToDouble(ObraTarea::getHorasAyudantes).sum());
            h.setHoras(h.getHorasOficiales()+h.getHorasAyudantes());
            h.setCantidadReal(group.stream().mapToDouble(ObraTarea::getCantidadReal).sum());
            h.setCantidadPrevista(group.stream().mapToDouble(ObraTarea::getCantidadPrevista).sum());
            h.setHorasPrevistas(h.getCantidadReal()*(r.getHorasOficialesPrevistas()+r.getHorasAyudantesPrevistas())/r.getCantidad());
            h.setDescripcion(MARKER+"RESUMEN:"+r.getnRubro()+":"+g+" · resumen de HH de cuadrilla, no carga adicional del cronograma");
            if (existing.isEmpty()) p.getRegistrosHoras().add(h);
        }
    }

    private static boolean completeRestrictions(Proyecto p) {
        String[] names={"Retiro de caja de escaleras","Pedido de acero para vigas","Hidrosanitario nivel P1",
            "Acondicionamiento de cerramiento","Aprobación de pintura exterior"};
        String[] oldTypes={"Logística","Materiales","Instalación","Ejecutivo","Calidad"};
        String[] newTypes={"Equipos","Materiales","Documentacion","Mano de obra","Documentacion"};
        boolean changed=false;
        for(int i=0;i<names.length;i++) {
            final int index=i;
            var activity=p.getRubros().stream().filter(r -> String.valueOf(5000+index).equals(r.getnRubro())).findFirst().orElse(null);
            if(activity==null || activity.getFechaInicioPlanificada()==null) continue;
            for(var restriction:p.getRestricciones()) {
                if(!names[i].equals(restriction.getRestriccion()) || !oldTypes[i].equals(restriction.getTipo())
                    || !LocalDate.of(2026,8,5+i).equals(restriction.getFechaSolicitud())
                    || !LocalDate.of(2026,8,15+i*4).equals(restriction.getFechaMaximaEntrega())
                    || !(i%2==0?"Supervisión":"Compras").equals(restriction.getResponsable())
                    || !Objects.equals(i==4?LocalDate.of(2026,8,20):null,restriction.getFechaRealEntrega())
                    || !(i==4?"RESUELTO":i%2==0?"PENDIENTE":"EN_PROCESO").equals(restriction.getEstado())) continue;
                var start=activity.getFechaInicioPlanificada();
                restriction.setTipo(newTypes[i]); restriction.setNivel(activity.getNivel()); restriction.setUnidad(activity.getUnidad());
                restriction.setResponsable(i%2==0?"Laura Torres":"Marina Sosa");
                restriction.setFechaSolicitud(start); restriction.setFechaMaximaEntrega(start.plusDays(7));
                restriction.setEstado(i<2?"RESUELTO":"PENDIENTE"); restriction.setFechaRealEntrega(i<2?start.plusDays(5):null);
                changed=true;
            }
        }
        return changed;
    }

    private static void completeDeliveryProgress(Proyecto p) {
        // Only known original dashboard fixtures, identified by their original costs.
        String[][] originals = {
            {"Fundación - Base","85","EN_PROCESO","2026-08-01","2026-09-15","1800000"},
            {"Vigas - Estructura","92","EJECUTADO","2026-08-10","2026-09-25","2200000"},
            {"Losas - Estructura","78","EN_PROCESO","2026-09-01","2026-10-20","1750000"},
            {"Mampostería - Muros","52","PENDIENTE","2026-10-05","2026-11-18","980000"},
            {"Pintura - Acabado","38","PENDIENTE","2026-11-02","2026-12-20","760000"},
            {"Sanitarios - Instalaciones","69","EN_PROCESO","2026-08-15","2026-10-15","1350000"},
            {"Eléctricas - Instalaciones","44","PENDIENTE","2026-09-10","2026-11-25","1200000"},
            {"Pisos - Acabados","34","PENDIENTE","2026-10-12","2026-12-05","830000"}};
        for (var delivery : p.getEntregas()) for (var task : delivery.getTareas()) {
            boolean original = Arrays.stream(originals).anyMatch(v -> v[0].equals(task.getNombre())
                && Objects.equals(Integer.valueOf(v[1]),task.getAvancePct()) && StatusTarea.valueOf(v[2])==task.getStatus()
                && LocalDate.parse(v[3]).equals(task.getFechaInicio()) && LocalDate.parse(v[4]).equals(task.getFechaFin())
                && Objects.equals(Double.valueOf(v[5]),task.getCosto()));
            if (!original) continue;
            var matches = p.getRubros().stream().filter(r -> Objects.equals(r.getRubro(),task.getSistema())).toList();
            if (matches.size()!=1) continue;
            var r = matches.get(0);
            var daily = p.getObraTareas().stream().filter(t -> t.getDescripcion()!=null && t.getDescripcion().startsWith(MARKER+r.getnRubro()+":")).toList();
            if (daily.isEmpty()) continue;
            double quantity = daily.stream().mapToDouble(ObraTarea::getCantidadReal).sum();
            int progress = (int)Math.round(quantity/r.getCantidad()*100);
            task.setAvancePct(progress); task.setStatus(progress>=100?StatusTarea.EJECUTADO:progress>0?StatusTarea.EN_PROCESO:StatusTarea.PENDIENTE);
            task.setFechaInicio(r.getFechaInicioPlanificada()); task.setFechaFin(r.getFechaFinPlanificada());
        }
    }

    private static LocalDate nextWorkingDay(LocalDate d) { while(d.getDayOfWeek()==DayOfWeek.SATURDAY || d.getDayOfWeek()==DayOfWeek.SUNDAY) d=d.plusDays(1); return d; }
    private static List<LocalDate> workingDays(LocalDate start,LocalDate end) {
        var dates = new ArrayList<LocalDate>();
        for (var d = start; !d.isAfter(end); d=d.plusDays(1)) if(d.getDayOfWeek()!=DayOfWeek.SATURDAY && d.getDayOfWeek()!=DayOfWeek.SUNDAY) dates.add(d);
        return dates;
    }
    private static boolean valid(Double n) { return n!=null && Double.isFinite(n) && n>=0; }
    private static boolean positive(Double n) { return valid(n) && n>0; }
    private static double safe(Double n) { return valid(n)?n:0.; }
}
