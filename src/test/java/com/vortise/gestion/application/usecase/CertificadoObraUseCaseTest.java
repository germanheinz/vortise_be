package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vortise.gestion.application.dto.CertificadoCrearDto;
import com.vortise.gestion.application.dto.CertificadoDetalleDto;
import com.vortise.gestion.application.dto.CertificadoFilaDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CertificadoItemCompletado;
import com.vortise.gestion.domain.model.CertificadoObra;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.CertificadoItemCompletadoRepository;
import com.vortise.gestion.domain.repository.CertificadoObraRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CertificadoObraUseCaseTest {
    private static final long FUNDACIONES = 2L, LOSAS = 3L, HIERRO = 4L;
    private final Proyecto project = new Proyecto(), other = new Proyecto();
    private final CatalogoEmpresa sub = mock(CatalogoEmpresa.class);
    private final FakeCertificados certificates = new FakeCertificados();
    private final FakeCompletados completed = new FakeCompletados();
    private final FakeTareas loads = new FakeTareas();
    private final CertificadoObraUseCase useCase;
    private final CertificadoBloqueos locks = new CertificadoBloqueos(certificates, completed);

    CertificadoObraUseCaseTest() {
        project.setId(1L); project.setEmpresa("Constructora"); other.setId(2L);
        when(sub.getId()).thenReturn(7L); when(sub.getNombre()).thenReturn("Hormigones SA");
        project.setSubcontratistas(Set.of(sub));
        project.setRubros(List.of(rubro(1L, "5000", "Hormigón", null, null, null, false),
            rubro(FUNDACIONES, "5001", "Hormigón", "Fundaciones", 100.0, "m3", false),
            rubro(LOSAS, "5002", "Hormigón", "Losas", 50.0, "m2", true),
            rubro(HIERRO, "6001", "Hierro", null, 10.0, "kg", false)));
        var projects = mock(ProyectoRepository.class);
        when(projects.findById(1L)).thenReturn(Optional.of(project)); when(projects.findById(2L)).thenReturn(Optional.of(other));
        other.setRubros(List.of());
        var identity = mock(CurrentIdentity.class); when(identity.usuarioId()).thenReturn(5L);
        var users = mock(UsuarioEmpresaRepository.class);
        var ana = mock(UsuarioEmpresa.class); when(ana.getNombre()).thenReturn("Ana");
        when(users.findById(5L)).thenReturn(Optional.of(ana));
        useCase = new CertificadoObraUseCase(projects, certificates, completed, loads, identity, users);
    }

    private RubroObra rubro(Long id, String code, String name, String sub, Double quantity, String unit, boolean subcontract) {
        var r = new RubroObra(); r.setId(id); r.setnRubro(code); r.setRubro(name); r.setSubRubro(sub); r.setCantidad(quantity); r.setUnidad(unit);
        r.setProyecto(project);
        if (subcontract) { r.setTipoContratista("SUBCONTRATO"); r.setSubcontratista(this.sub); }
        return r;
    }

    private void load(long rubroId, String date, double quantity) {
        var t = new ObraTarea(); t.setId((long) loads.all.size() + 1); t.setProyecto(project);
        var r = new RubroObra(); r.setId(rubroId); t.setActividad(r);
        t.setFecha(LocalDate.parse(date)); t.setCantidadReal(quantity); t.setRegistradoPorUsuarioId(5L); loads.all.add(t);
    }

    private CertificadoCrearDto monthly(String cutoff, Integer first) { return new CertificadoCrearDto(null, "MENSUAL", LocalDate.parse(cutoff), first); }
    private CertificadoFilaDto row(CertificadoDetalleDto detail, long rubroId) { return detail.filas().stream().filter(f -> f.rubroId() == rubroId).findFirst().orElseThrow(); }

    @Test void listsCompaniesWithOwnLaborFirstAndOnlyActivitiesAreItems() {
        var companies = useCase.empresas(1L);
        assertEquals(2, companies.size());
        assertTrue(companies.get(0).propia()); assertEquals("Constructora (propia)", companies.get(0).nombre()); assertEquals(2, companies.get(0).actividades());
        assertEquals(7L, companies.get(1).subcontratistaId()); assertEquals(1, companies.get(1).actividades());
        var detail = useCase.crear(1L, monthly("2026-03-31", 3));
        assertEquals(List.of(FUNDACIONES, HIERRO), detail.filas().stream().map(CertificadoFilaDto::rubroId).toList());
        assertEquals("5000", row(detail, FUNDACIONES).grupoCodigo()); assertEquals("Hormigón", row(detail, FUNDACIONES).grupoNombre()); assertNull(row(detail, HIERRO).grupoCodigo());
        assertEquals("Fundaciones", row(detail, FUNDACIONES).nombre()); assertEquals("Hierro", row(detail, HIERRO).nombre());
    }

    @Test void newCertificateStartsInZeroAndMonthlyNumberingContinuesAutomatically() {
        var first = useCase.crear(1L, monthly("2026-03-31", 5));
        assertEquals(5, first.numero()); assertEquals("Certificado N° 5", first.etiqueta());
        assertEquals(0, row(first, FUNDACIONES).acumuladoCant()); assertEquals(0.0, row(first, FUNDACIONES).acumuladoPct());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, monthly("2026-04-30", null)));
        useCase.cerrar(1L, first.id());
        var second = useCase.crear(1L, monthly("2026-04-30", 99));
        assertEquals(6, second.numero());
        assertEquals(LocalDate.parse("2026-04-01"), second.periodoDesde());
    }

    @Test void rejectsInvalidCreations() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, new CertificadoCrearDto(99L, "MENSUAL", LocalDate.parse("2026-03-31"), 1)));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, new CertificadoCrearDto(null, "SEMANAL", LocalDate.parse("2026-03-31"), 1)));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, monthly("2026-03-31", 0)));
        var first = useCase.crear(1L, monthly("2026-03-31", 1)); useCase.cerrar(1L, first.id());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, monthly("2026-03-31", null)));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, monthly("2026-03-01", null)));
    }

    @Test void fortnightCertificatesAreNamedByCutoffAndFortnightCannotRepeat() {
        var q1 = useCase.crear(1L, new CertificadoCrearDto(null, "QUINCENAL", LocalDate.parse("2026-03-03"), null));
        assertEquals("Quincena 1 – Marzo", q1.etiqueta());
        useCase.cerrar(1L, q1.id());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.crear(1L, new CertificadoCrearDto(null, "QUINCENAL", LocalDate.parse("2026-03-10"), null)));
        var q2 = useCase.crear(1L, new CertificadoCrearDto(null, "QUINCENAL", LocalDate.parse("2026-03-20"), null));
        assertEquals("Quincena 2 – Marzo", q2.etiqueta()); assertEquals(2, q2.numero());
        useCase.cerrar(1L, q2.id());
        assertEquals("Quincena 1 – Abril", useCase.crear(1L, new CertificadoCrearDto(null, "QUINCENAL", LocalDate.parse("2026-04-05"), null)).etiqueta());
    }

    @Test void monthlyExampleAndEachLoadCountsInOneCertificateOnly() {
        load(FUNDACIONES, "2026-03-10", 10); load(FUNDACIONES, "2026-04-10", 10); load(FUNDACIONES, "2026-03-31", 5);
        var march = useCase.crear(1L, monthly("2026-03-31", 1));
        assertEquals(15, row(march, FUNDACIONES).actualCant()); assertEquals(15.0, row(march, FUNDACIONES).actualPct());
        useCase.cerrar(1L, march.id());
        var april = useCase.crear(1L, monthly("2026-04-30", null));
        var fila = row(april, FUNDACIONES);
        assertEquals(15, fila.anteriorCant()); assertEquals(10, fila.actualCant()); assertEquals(25, fila.acumuladoCant()); assertEquals(25.0, fila.acumuladoPct());
        assertEquals(15, row(useCase.detalle(1L, march.id()), FUNDACIONES).acumuladoCant());
    }

    @Test void subcontractedCompanyHasItsOwnCertificates() {
        load(LOSAS, "2026-03-10", 20);
        var detail = useCase.crear(1L, new CertificadoCrearDto(7L, "MENSUAL", LocalDate.parse("2026-03-31"), 1));
        assertEquals("Hormigones SA", detail.empresa()); assertFalse(detail.propia());
        assertEquals(List.of(LOSAS), detail.filas().stream().map(CertificadoFilaDto::rubroId).toList());
        assertEquals(40.0, detail.filas().get(0).acumuladoPct());
        assertTrue(useCase.listar(1L, null).isEmpty()); assertEquals(1, useCase.listar(1L, 7L).size());
    }

    @Test void cannotCloseWithFutureCutoffOrTwice() {
        var future = useCase.crear(1L, monthly(LocalDate.now().plusDays(3).toString(), 1));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.cerrar(1L, future.id()));
        var past = useCase.crear(1L, new CertificadoCrearDto(7L, "MENSUAL", LocalDate.parse("2026-03-31"), 1));
        useCase.cerrar(1L, past.id());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.cerrar(1L, past.id()));
    }

    @Test void editingTheCutoffMovesLoadsBetweenCertificatesWithoutDuplicatingThem() {
        load(FUNDACIONES, "2026-03-30", 3); load(FUNDACIONES, "2026-04-02", 4); load(FUNDACIONES, "2026-04-20", 5);
        var march = useCase.crear(1L, monthly("2026-03-31", 1)); useCase.cerrar(1L, march.id());
        var april = useCase.crear(1L, monthly("2026-04-30", null)); useCase.cerrar(1L, april.id());
        var moved = useCase.editarCorte(1L, march.id(), LocalDate.parse("2026-04-03"));
        assertEquals(7, row(moved, FUNDACIONES).actualCant());
        var next = useCase.detalle(1L, april.id());
        assertEquals(5, row(next, FUNDACIONES).actualCant()); assertEquals(12, row(next, FUNDACIONES).acumuladoCant());
        assertEquals("CERRADO", moved.estado());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.editarCorte(1L, march.id(), LocalDate.parse("2026-04-30")));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.editarCorte(1L, april.id(), LocalDate.parse("2026-04-03")));
    }

    @Test void excessAboveTheContractIsInformativeAndListedWithUserAndDate() {
        load(FUNDACIONES, "2026-03-10", 95); load(FUNDACIONES, "2026-03-25", 8);
        var march = useCase.crear(1L, monthly("2026-03-31", 1));
        var fila = row(march, FUNDACIONES);
        assertEquals(100, fila.acumuladoCant()); assertEquals(100, fila.actualCant());
        assertTrue(march.tieneExcedente()); assertEquals(3, fila.excedenteCant()); assertEquals(3.0, fila.excedentePct());
        var table = useCase.excedentes(1L);
        assertEquals(1, table.size());
        var entry = table.get(0);
        assertEquals("EXCEDENTE", entry.tipo()); assertEquals(103, entry.cantidadEjecutada()); assertEquals(3, entry.cantidad()); assertEquals(100.0, entry.cantidadContratada());
        assertEquals("Ana", entry.usuario()); assertEquals(LocalDate.parse("2026-03-25"), entry.fecha()); assertEquals("Fundaciones", entry.item());
    }

    @Test void completingAnItemClosesItAtTotalAndBlocksFurtherLoadsUntilUnmarked() {
        load(FUNDACIONES, "2026-03-10", 97);
        var march = useCase.crear(1L, monthly("2026-03-31", 1));
        assertTrue(row(march, FUNDACIONES).puedeAlternarCompletado());
        var marked = useCase.marcarCompletado(1L, march.id(), FUNDACIONES, true);
        var fila = row(marked, FUNDACIONES);
        assertEquals(100, fila.acumuladoCant()); assertEquals(3, fila.ajusteCant()); assertEquals(3.0, fila.ajustePct()); assertTrue(fila.completado()); assertTrue(marked.tieneAjuste());
        assertThrows(SolicitudInvalidaException.class, () -> locks.validarAvance(1L, project.getRubros().get(1), LocalDate.parse("2026-04-10")));
        var table = useCase.excedentes(1L);
        assertEquals("AJUSTE", table.get(0).tipo()); assertEquals(97, table.get(0).cantidadEjecutada()); assertEquals("Ana", table.get(0).usuario());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.marcarCompletado(1L, march.id(), FUNDACIONES, true));
        var unmarked = useCase.marcarCompletado(1L, march.id(), FUNDACIONES, false);
        assertEquals(97, row(unmarked, FUNDACIONES).acumuladoCant()); assertFalse(unmarked.tieneAjuste());
        assertDoesNotThrow(() -> locks.validarAvance(1L, project.getRubros().get(1), LocalDate.parse("2026-04-10")));
    }

    @Test void completionRulesProtectClosedCertificatesAndFullItems() {
        load(FUNDACIONES, "2026-03-10", 100);
        var march = useCase.crear(1L, monthly("2026-03-31", 1));
        assertFalse(row(march, FUNDACIONES).puedeAlternarCompletado());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.marcarCompletado(1L, march.id(), FUNDACIONES, true));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.marcarCompletado(1L, march.id(), LOSAS, true));
        var done = useCase.marcarCompletado(1L, march.id(), HIERRO, true);
        assertTrue(row(done, HIERRO).completado());
        useCase.cerrar(1L, march.id());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.marcarCompletado(1L, march.id(), HIERRO, false));
        var april = useCase.crear(1L, monthly("2026-04-30", null));
        assertEquals(10, row(april, HIERRO).acumuladoCant()); assertEquals(0, row(april, HIERRO).actualCant()); assertEquals(0, row(april, HIERRO).ajusteCant());
        assertFalse(row(april, HIERRO).puedeAlternarCompletado());
    }

    @Test void loadsInsideAClosedPeriodOfTheirCompanyAreRejectedButOtherCompaniesAreNot() {
        var march = useCase.crear(1L, monthly("2026-03-31", 1)); useCase.cerrar(1L, march.id());
        var own = project.getRubros().get(1); var subcontracted = project.getRubros().get(2);
        assertThrows(SolicitudInvalidaException.class, () -> locks.validarAvance(1L, own, LocalDate.parse("2026-03-31")));
        assertDoesNotThrow(() -> locks.validarAvance(1L, own, LocalDate.parse("2026-04-01")));
        assertDoesNotThrow(() -> locks.validarAvance(1L, subcontracted, LocalDate.parse("2026-03-15")));
    }

    @Test void activitiesWithProgressInAClosedCertificateCannotChangeQuantityUnitCompanyOrBeRemoved() {
        load(FUNDACIONES, "2026-03-10", 10);
        var own = project.getRubros().get(1);
        RubroCreateDto same = request(own, 100.0, "m3", null), quantity = request(own, 120.0, "m3", null), company = request(own, 100.0, "m3", 7L);
        assertDoesNotThrow(() -> locks.validarCambioActividad(1L, own, quantity, loads.all));
        var march = useCase.crear(1L, monthly("2026-03-31", 1)); useCase.cerrar(1L, march.id());
        assertDoesNotThrow(() -> locks.validarCambioActividad(1L, own, same, loads.all));
        assertThrows(SolicitudInvalidaException.class, () -> locks.validarCambioActividad(1L, own, quantity, loads.all));
        assertThrows(SolicitudInvalidaException.class, () -> locks.validarCambioActividad(1L, own, company, loads.all));
        assertThrows(SolicitudInvalidaException.class, () -> locks.validarCambioActividad(1L, own, null, loads.all));
        assertDoesNotThrow(() -> locks.validarCambioActividad(1L, project.getRubros().get(3), null, loads.all));
    }

    private static RubroCreateDto request(RubroObra r, Double quantity, String unit, Long subcontractor) {
        return new RubroCreateDto(r.getnRubro(), r.getRubro(), null, null, r.getSubRubro(), quantity, unit, null, null, subcontractor == null ? "PROPIA" : "SUBCONTRATO",
            subcontractor, null, null, null, null, null, false, r.getId(), null, null);
    }

    @Test void certificatesAreScopedToTheirObra() {
        var march = useCase.crear(1L, monthly("2026-03-31", 1));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.detalle(2L, march.id()));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.cerrar(2L, march.id()));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.empresas(99L));
    }

    static class FakeCertificados implements CertificadoObraRepository {
        final List<CertificadoObra> all = new ArrayList<>();
        public List<CertificadoObra> findByProyectoIdOrderByFechaCorteAscIdAsc(Long id) {
            return all.stream().filter(c -> c.getProyecto().getId().equals(id))
                .sorted(Comparator.comparing(CertificadoObra::getFechaCorte).thenComparing(CertificadoObra::getId)).toList();
        }
        public Optional<CertificadoObra> findByIdAndProyectoId(Long id, Long projectId) {
            return all.stream().filter(c -> c.getId().equals(id) && c.getProyecto().getId().equals(projectId)).findFirst();
        }
        public CertificadoObra save(CertificadoObra c) { if (c.getId() == null) { c.setId((long) all.size() + 1); all.add(c); } return c; }
    }

    static class FakeCompletados implements CertificadoItemCompletadoRepository {
        final List<CertificadoItemCompletado> all = new ArrayList<>();
        public List<CertificadoItemCompletado> findByCertificadoProyectoId(Long id) { return all.stream().filter(i -> i.getCertificado().getProyecto().getId().equals(id)).toList(); }
        public Optional<CertificadoItemCompletado> findByCertificadoIdAndRubroId(Long c, Long r) {
            return all.stream().filter(i -> i.getCertificado().getId().equals(c) && i.getRubroId().equals(r)).findFirst();
        }
        public CertificadoItemCompletado save(CertificadoItemCompletado i) { if (i.getId() == null) { i.setId((long) all.size() + 1); all.add(i); } return i; }
        public void delete(CertificadoItemCompletado i) { all.remove(i); }
    }

    static class FakeTareas implements ObraTareaRepository {
        final List<ObraTarea> all = new ArrayList<>();
        public List<ObraTarea> findByProyectoId(Long id) { return all.stream().filter(t -> t.getProyecto().getId().equals(id)).toList(); }
        public Optional<ObraTarea> findById(Long id) { return all.stream().filter(t -> t.getId().equals(id)).findFirst(); }
        public ObraTarea save(ObraTarea t) { all.add(t); return t; }
        public void deleteById(Long id) { all.removeIf(t -> t.getId().equals(id)); }
    }
}
