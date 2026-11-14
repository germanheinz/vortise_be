package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vortise.gestion.application.dto.ReporteEmitirDto;
import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;
import com.vortise.gestion.application.port.ImagenReporteStorage;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.ReporteEmitido;
import com.vortise.gestion.domain.model.ReporteImagen;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.model.VarianteEstructura;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.ReporteEmitidoRepository;
import com.vortise.gestion.domain.repository.ReporteImagenRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ReporteObraUseCaseTest {
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private final ObjectMapper mapper = new ObjectMapper();
    private final Proyecto project = new Proyecto();
    private final ProyectoRepository projects = mock(ProyectoRepository.class);
    private final CurrentIdentity identity = mock(CurrentIdentity.class);
    private final UsuarioEmpresaRepository users = mock(UsuarioEmpresaRepository.class);
    private final FakeStorage storage = new FakeStorage();
    private final FakeReports reports = new FakeReports();
    private final FakeImages images = new FakeImages();
    private final ReporteObraUseCase useCase;

    ReporteObraUseCaseTest() {
        project.setId(1L);
        var other = new Proyecto(); other.setId(2L);
        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(projects.findById(2L)).thenReturn(Optional.of(other));
        when(identity.usuarioId()).thenReturn(7L);
        var user = mock(UsuarioEmpresa.class); when(user.getNombre()).thenReturn("Ana");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        useCase = new ReporteObraUseCase(projects, reports, images, storage, identity, users, mapper);
    }

    private ReporteEmitirDto emit(String json) {
        return new ReporteEmitirDto(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 9), mapper.readTree(json));
    }

    @Test void uploadsValidImageAsDraftAndKeepsOrder() {
        var first = useCase.subirImagen(1L, PNG, "  Losa\n nivel 2 ");
        var second = useCase.subirImagen(1L, PNG, null);
        assertEquals(1, first.orden()); assertEquals(2, second.orden());
        assertEquals("Losa  nivel 2", first.leyenda()); assertNull(second.leyenda());
        assertEquals(2, useCase.borrador(1L).imagenes().size());
        assertTrue(useCase.borrador(1L).subidaDisponible());
    }

    private RubroObra node(long id, String code, TipoNodoRubro type, RubroObra parent, String name) {
        RubroObra node = new RubroObra();
        node.setId(id); node.setnRubro(code); node.setTipoNodo(type); node.setNombre(name); node.setRubro("Estructura");
        node.setParent(parent); node.setProyecto(project);
        project.getRubros().add(node);
        return node;
    }

    @Test void storesDateRubroAndNivelOfTheImageWithTheirNames() {
        RubroObra rubro = node(10, "1", TipoNodoRubro.RUBRO, null, "Estructura");
        rubro.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        RubroObra nivel = node(11, "1.1", TipoNodoRubro.NIVEL, rubro, "Fundaciones");
        node(12, "1.1.1", TipoNodoRubro.SUBRUBRO, nivel, "Platea");

        var onNivel = useCase.subirImagen(1L, PNG, null, LocalDate.of(2026, 11, 14), 10L, 11L);
        var onRubro = useCase.subirImagen(1L, PNG, null, LocalDate.of(2026, 11, 14), 10L, null);

        assertEquals(LocalDate.of(2026, 11, 14), onNivel.fecha());
        assertEquals(10L, onNivel.rubroId()); assertEquals("Estructura", onNivel.rubroNombre());
        assertEquals(11L, onNivel.nivelId()); assertEquals("Fundaciones", onNivel.nivelNombre());
        assertNull(onRubro.nivelId()); assertNull(onRubro.nivelNombre());
        assertNull(useCase.subirImagen(1L, PNG, null).rubroId());
    }

    @Test void rejectsRubroAndNivelThatDoNotMatchTheStructureOfTheObra() {
        RubroObra rubro = node(10, "1", TipoNodoRubro.RUBRO, null, "Estructura");
        rubro.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        RubroObra nivel = node(11, "1.1", TipoNodoRubro.NIVEL, rubro, "Fundaciones");
        node(12, "1.1.1", TipoNodoRubro.SUBRUBRO, nivel, "Platea");
        RubroObra otherRubro = node(20, "2", TipoNodoRubro.RUBRO, null, "Terminaciones");
        otherRubro.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        node(21, "2.1", TipoNodoRubro.NIVEL, otherRubro, "PB");

        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, null, null, 999L, null));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, null, null, 11L, null));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, null, null, null, 11L));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, null, null, 10L, 21L));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, null, null, 10L, 12L));
        assertEquals(0, useCase.borrador(1L).imagenes().size());
    }

    @Test void rejectsUnconfiguredStorageEmptyOversizedAndUnknownFiles() {
        storage.available = false;
        assertThrows(FeatureUnavailableException.class, () -> useCase.subirImagen(1L, PNG, null));
        assertFalse(useCase.borrador(1L).subidaDisponible());
        storage.available = true;
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, new byte[0], null));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, "<svg/>".getBytes(), null));
        var big = new byte[(int) ReporteObraUseCase.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, big, null));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, "x".repeat(256)));
        assertTrue(storage.uploaded.isEmpty());
    }

    @Test void limitsDraftImagesPerReport() {
        for (int i = 0; i < ReporteObraUseCase.MAX_IMAGENES; i++) useCase.subirImagen(1L, PNG, null);
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirImagen(1L, PNG, null));
        assertEquals(ReporteObraUseCase.MAX_IMAGENES, storage.uploaded.size());
    }

    @Test void removesStoredFileIfSavingTheRowFails() {
        images.failNextSave = true;
        assertThrows(IllegalStateException.class, () -> useCase.subirImagen(1L, PNG, null));
        assertEquals(storage.uploaded, storage.deleted);
    }

    @Test void deletesDraftsAndKeepsDatabaseConsistentWhenStorageFails() {
        var image = useCase.subirImagen(1L, PNG, null);
        storage.failDeletes = true;
        useCase.eliminarImagen(1L, image.id());
        assertTrue(useCase.borrador(1L).imagenes().isEmpty());
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.eliminarImagen(1L, image.id()));
    }

    @Test void imagesOfAnotherObraAreNotReachable() {
        var image = useCase.subirImagen(1L, PNG, null);
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.eliminarImagen(2L, image.id()));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.actualizarLeyenda(2L, image.id(), "x"));
        assertEquals(1, useCase.borrador(1L).imagenes().size());
    }

    @Test void issuingNumbersReportsAttachesDraftsAndFreezesTheSnapshot() {
        var image = useCase.subirImagen(1L, PNG, "Vista");
        var first = useCase.emitir(1L, emit("{\"avance\":{\"obra\":40}}"));
        assertEquals(1, first.numero()); assertEquals("Ana", first.emitidoPor());
        assertEquals(40, first.snapshot().get("avance").get("obra").asInt());
        assertEquals(List.of(image.id()), first.imagenes().stream().map(i -> i.id()).toList());
        assertTrue(useCase.borrador(1L).imagenes().isEmpty());

        var second = useCase.emitir(1L, emit("{}"));
        assertEquals(2, second.numero()); assertTrue(second.imagenes().isEmpty());
        var list = useCase.listar(1L);
        assertEquals(List.of(2, 1), list.stream().map(r -> r.numero()).toList());
        assertEquals(1, list.get(1).cantidadImagenes()); assertEquals(0, list.get(0).cantidadImagenes());
        assertEquals(1, useCase.detalle(1L, first.id()).imagenes().size());
        assertThrows(SolicitudInvalidaException.class, () -> useCase.eliminarImagen(1L, image.id()));
    }

    @Test void numbersAreIndependentPerObraAndReportsAreNotCrossReadable() {
        var report = useCase.emitir(1L, emit("{}"));
        assertEquals(1, useCase.emitir(2L, emit("{}")).numero());
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.detalle(2L, report.id()));
        assertEquals(1, useCase.listar(1L).size());
    }

    @Test void rejectsInvalidPeriodsAndSnapshots() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.emitir(1L, new ReporteEmitirDto(
            LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 3), mapper.readTree("{}"))));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.emitir(1L, new ReporteEmitirDto(
            LocalDate.of(2026, 11, 9), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 9), mapper.readTree("{}"))));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.emitir(1L, emit("[1,2]")));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.emitir(1L, emit("{\"texto\":\"" + "a".repeat(1_000_001) + "\"}")));
        assertTrue(useCase.listar(1L).isEmpty());
    }

    @Test void unknownObraIsNotFound() {
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.borrador(99L));
    }

    static class FakeStorage implements ImagenReporteStorage {
        boolean available = true, failDeletes = false;
        final List<String> uploaded = new ArrayList<>(), deleted = new ArrayList<>();
        public boolean disponible() { return available; }
        public Subida subir(Long proyectoId, byte[] datos, TipoImagen tipo) {
            String id = "gestion/reportes/" + proyectoId + "/" + uploaded.size();
            uploaded.add(id);
            return new Subida(id, "https://res.cloudinary.com/demo/image/upload/" + id + ".png", 800, 600, datos.length);
        }
        public void eliminar(String publicId) {
            deleted.add(publicId);
            if (failDeletes) throw new FeatureUnavailableException("down");
        }
    }

    static class FakeReports implements ReporteEmitidoRepository {
        final List<ReporteEmitido> all = new ArrayList<>();
        public List<ReporteEmitido> findByProyectoIdOrderByNumeroDesc(Long id) {
            return all.stream().filter(r -> r.getProyecto().getId().equals(id)).sorted((a, b) -> b.getNumero() - a.getNumero()).toList();
        }
        public Optional<ReporteEmitido> findByIdAndProyectoId(Long id, Long projectId) {
            return all.stream().filter(r -> r.getId().equals(id) && r.getProyecto().getId().equals(projectId)).findFirst();
        }
        public int ultimoNumero(Long id) { return all.stream().filter(r -> r.getProyecto().getId().equals(id)).mapToInt(ReporteEmitido::getNumero).max().orElse(0); }
        public ReporteEmitido save(ReporteEmitido report) {
            if (report.getId() == null) { report.setId((long) all.size() + 1); all.add(report); }
            return report;
        }
    }

    static class FakeImages implements ReporteImagenRepository {
        final List<ReporteImagen> all = new ArrayList<>();
        boolean failNextSave = false;
        public List<ReporteImagen> findByProyectoIdAndReporteIsNullOrderByOrdenAsc(Long id) {
            return all.stream().filter(i -> i.getProyecto().getId().equals(id) && i.getReporte() == null)
                .sorted((a, b) -> a.getOrden() - b.getOrden()).toList();
        }
        public List<ReporteImagen> findByReporteIdOrderByOrdenAsc(Long id) {
            return all.stream().filter(i -> i.getReporte() != null && i.getReporte().getId().equals(id)).sorted((a, b) -> a.getOrden() - b.getOrden()).toList();
        }
        public List<ReporteImagen> findByReporteIdIn(Collection<Long> ids) {
            return all.stream().filter(i -> i.getReporte() != null && ids.contains(i.getReporte().getId())).toList();
        }
        public Optional<ReporteImagen> findByIdAndProyectoId(Long id, Long projectId) {
            return all.stream().filter(i -> i.getId().equals(id) && i.getProyecto().getId().equals(projectId)).findFirst();
        }
        public ReporteImagen save(ReporteImagen image) {
            if (failNextSave) { failNextSave = false; throw new IllegalStateException("database down"); }
            if (image.getId() == null) { image.setId((long) all.size() + 1); all.add(image); }
            return image;
        }
        public void delete(ReporteImagen image) { all.remove(image); }
    }
}
