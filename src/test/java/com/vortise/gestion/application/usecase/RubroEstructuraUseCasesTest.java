package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.CertificadoCrearDto;
import com.vortise.gestion.application.dto.CertificadoFilaDto;
import com.vortise.gestion.application.dto.ObraTareaCreateDto;
import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.application.dto.ProyectoUpdateDto;
import com.vortise.gestion.application.dto.RegistroHorasCreateDto;
import com.vortise.gestion.application.dto.RestriccionCreateDto;
import com.vortise.gestion.application.dto.RubroCreateDto;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RegistroHoras;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.model.VarianteEstructura;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** 3-layer activity structure through the use cases and mappers. Mocks only: no Spring, no database. */
class RubroEstructuraUseCasesTest {
    private static final Long EMPRESA_ID = 7L;

    private static RubroCreateDto row(Long id, String code, String parent, String tipo, String variante, String nombre,
            Double cantidad, String unidad) {
        return new RubroCreateDto(code, null, null, null, null, cantidad, unidad, null, null, "PROPIA", null,
            null, null, null, null, null, false, id, null, null, null, tipo, parent, variante, nombre);
    }

    private static RubroObra stored(Proyecto project, Long id, String code, TipoNodoRubro tipo, RubroObra parent, String name) {
        RubroObra rubro = new RubroObra();
        rubro.setId(id);
        rubro.setProyecto(project);
        rubro.setnRubro(code);
        rubro.setTipoNodo(tipo);
        rubro.setParent(parent);
        rubro.setNombre(name);
        return rubro;
    }

    private static RubroObra byCode(Proyecto project, String code) {
        return project.getRubros().stream().filter(item -> code.equals(item.getnRubro())).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ create

    private ProyectoRepository projects = mock(ProyectoRepository.class);

    private CreateProyectoUseCase createUseCase() {
        EmpresaRepository companies = mock(EmpresaRepository.class);
        Empresa company = mock(Empresa.class);
        when(company.getId()).thenReturn(EMPRESA_ID);
        when(companies.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(company));
        var changes = mock(AplicarCambiosEmpresaObraUseCase.class);
        when(changes.execute(any(), any(), any())).thenReturn(List.of());
        when(projects.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new CreateProyectoUseCase(projects, companies, mock(CatalogoEmpresaRepository.class), mock(CurrentIdentity.class), changes);
    }

    private ProyectoCreateDto createRequest(List<RubroCreateDto> rubros) {
        return new ProyectoCreateDto("Obra", null, null, "Dirección", "OB-1", null, null, null, null, null, EMPRESA_ID,
            null, null, null, null, rubros, List.of());
    }

    @Test void createsTheThreeVariantsInTheSameObraWithoutDataOnGroupingNodes() {
        var result = createUseCase().execute(createRequest(List.of(
            row(null, "1", null, "RUBRO", "RUBRO_SUBRUBRO", "Estructura", 99.0, "m2"),
            row(null, "1.1", "1", "SUBRUBRO", null, "Losas", 10.0, "m2"),
            row(null, "2", null, "RUBRO", "RUBRO_NIVEL_SUBRUBRO", "Hormigón", 99.0, "m3"),
            row(null, "2.1", "2", "NIVEL", null, "General de la obra", 99.0, "m3"),
            row(null, "2.1.1", "2.1", "SUBRUBRO", null, "Subrubro 1", 5.0, "m3"),
            row(null, "2.1.2", "2.1", "SUBRUBRO", null, "Subrubro 2", 6.0, "m3"),
            row(null, "3", null, "RUBRO", "RUBRO_SUBRUBRO_NIVEL", "Terminaciones", null, null),
            row(null, "3.1", "3", "SUBRUBRO", null, "Subrubro", 99.0, "m2"),
            row(null, "3.1.1", "3.1", "NIVEL", null, "Subsuelo", 7.0, "m2"),
            row(null, "3.1.2", "3.1", "NIVEL", null, "P00", 8.0, "m2"))));

        ArgumentCaptor<Proyecto> saved = ArgumentCaptor.forClass(Proyecto.class);
        verify(projects).save(saved.capture());
        Proyecto project = saved.getValue();
        assertEquals(10, project.getRubros().size());

        // Parents are persisted before their children and linked by entity.
        for (RubroObra rubro : project.getRubros()) {
            if (rubro.getParent() != null) assertTrue(project.getRubros().indexOf(rubro.getParent()) < project.getRubros().indexOf(rubro));
        }
        assertSame(byCode(project, "2.1"), byCode(project, "2.1.1").getParent());
        assertSame(byCode(project, "3.1"), byCode(project, "3.1.2").getParent());
        assertNull(byCode(project, "1").getParent());

        // Grouping nodes carry no activity data even if the client sent it.
        for (String grouping : List.of("1", "2", "2.1", "3", "3.1")) {
            RubroObra node = byCode(project, grouping);
            assertNull(node.getCantidad(), grouping); assertNull(node.getUnidad(), grouping);
            assertNull(node.getSubRubro(), grouping); assertNull(node.getNivel(), grouping);
        }
        RubroObra leaf = byCode(project, "2.1.1");
        assertEquals(5.0, leaf.getCantidad()); assertEquals("m3", leaf.getUnidad());
        assertEquals("Hormigón", leaf.getRubro()); assertEquals("Subrubro 1", leaf.getSubRubro());
        assertEquals("General de la obra", leaf.getNivel());
        assertEquals("Subsuelo", byCode(project, "3.1.1").getSubRubro());
        assertEquals("P00", byCode(project, "3.1.2").getNivel());
        assertEquals(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO, byCode(project, "2").getVarianteEstructura());
        assertEquals(TipoNodoRubro.NIVEL, byCode(project, "2.1").getTipoNodo());

        // Response: natural code order, structure fields and leaf flag.
        assertEquals(List.of("1", "1.1", "2", "2.1", "2.1.1", "2.1.2", "3", "3.1", "3.1.1", "3.1.2"),
            result.rubros().stream().map(item -> item.nRubro()).toList());
        assertEquals(List.of(false, true, false, false, true, true, false, false, true, true),
            result.rubros().stream().map(item -> item.hoja()).toList());
        var root2 = result.rubros().get(2);
        assertEquals("RUBRO", root2.tipoNodo()); assertEquals("RUBRO_NIVEL_SUBRUBRO", root2.varianteEstructura());
        assertEquals("Hormigón", root2.nombre());
        assertEquals("NIVEL", result.rubros().get(3).tipoNodo()); assertNull(result.rubros().get(3).varianteEstructura());
    }

    @Test void createRejectsInvalidTreesWithoutSaving() {
        var useCase = createUseCase();
        assertThrows(SolicitudInvalidaException.class, () -> useCase.execute(createRequest(List.of(
            row(null, "1", null, "RUBRO", null, "A", null, null), row(null, "1.1", "9", "SUBRUBRO", null, "B", 1.0, "m")))));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.execute(createRequest(List.of(
            row(null, "1", null, "RUBRO", null, "A", null, null), row(null, "1", null, "RUBRO", null, "B", null, null)))));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.execute(createRequest(List.of(
            row(null, "1", null, "RUBRO", "RUBRO_SUBRUBRO", "A", null, null), row(null, "1.1", "1", "NIVEL", null, "B", 1.0, "m")))));
        verify(projects, never()).save(any(Proyecto.class));
    }

    @Test void createStillAcceptsLegacyPayloadsWithoutKind() {
        RubroCreateDto root = new RubroCreateDto("5000", "Hormigón", null, null, null, 99.0, "m3", null, null, "PROPIA", null,
            null, null, null, null, null, false, null, null, null);
        RubroCreateDto leaf = new RubroCreateDto("5001", "Hormigón", "PB", null, "Fundación", 10.0, "m3", null, null, "PROPIA", null,
            null, null, null, null, null, false, null, null, null);

        var result = createUseCase().execute(createRequest(List.of(leaf, root)));

        ArgumentCaptor<Proyecto> saved = ArgumentCaptor.forClass(Proyecto.class);
        verify(projects).save(saved.capture());
        Proyecto project = saved.getValue();
        assertEquals("5000", project.getRubros().get(0).getnRubro());
        assertEquals(TipoNodoRubro.RUBRO, byCode(project, "5000").getTipoNodo());
        assertNull(byCode(project, "5000").getCantidad());
        assertSame(byCode(project, "5000"), byCode(project, "5001").getParent());
        assertEquals("Fundación", byCode(project, "5001").getSubRubro());
        assertEquals("PB", byCode(project, "5001").getNivel());
        assertEquals(10.0, byCode(project, "5001").getCantidad());
        assertEquals(List.of(false, true), result.rubros().stream().map(item -> item.hoja()).toList());
    }

    // ------------------------------------------------------------------ update

    private ObraTareaRepository tasks = mock(ObraTareaRepository.class);
    private RestriccionObraRepository restrictions = mock(RestriccionObraRepository.class);
    private Proyecto project;
    private RubroObra root;
    private RubroObra leaf;
    private UpdateProyectoUseCase updateUseCase;

    @BeforeEach void setUpUpdate() {
        project = new Proyecto();
        project.setId(1L);
        Empresa company = new Empresa("Test", null);
        ReflectionTestUtils.setField(company, "id", 1L);
        project.setEmpresaRelacionada(company);
        root = stored(project, 10L, "1", TipoNodoRubro.RUBRO, null, "Estructura");
        root.setRubro("Estructura"); root.setVarianteEstructura(VarianteEstructura.RUBRO_SUBRUBRO);
        leaf = stored(project, 11L, "1.1", TipoNodoRubro.SUBRUBRO, root, "Losas");
        leaf.setRubro("Estructura"); leaf.setSubRubro("Losas"); leaf.setUnidad("m2"); leaf.setCantidad(10.0);
        project.setRubros(new ArrayList<>(List.of(root, leaf)));
        when(projects.bloquearPorId(1L)).thenReturn(Optional.of(project));
        when(projects.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tasks.findByProyectoId(1L)).thenReturn(List.of());
        updateUseCase = new UpdateProyectoUseCase(projects, mock(EmpresaRepository.class), mock(CatalogoEmpresaRepository.class),
            tasks, restrictions, mock(CurrentIdentity.class), mock(RegistroHorasRepository.class));
    }

    private ProyectoUpdateDto updateRequest(RubroCreateDto... rubros) {
        return new ProyectoUpdateDto("Obra", null, null, "Dirección", "OB-1", null, null, null, null, null, null, null, null, null,
            List.of(), java.util.Arrays.asList(rubros), null);
    }

    private void taskOn(RubroObra activity) {
        ObraTarea task = new ObraTarea();
        task.setActividad(activity);
        when(tasks.findByProyectoId(1L)).thenReturn(List.of(task));
    }

    @Test void updateAddsALeafUnderAnExistingRootKeepingIds() {
        var result = updateUseCase.execute(1L, updateRequest(
            row(10L, "1", null, "RUBRO", null, "Estructura", null, null),
            row(11L, "1.1", "1", "SUBRUBRO", null, "Losas", 10.0, "m2"),
            row(null, "1.2", "1", "SUBRUBRO", null, "Vigas", 5.0, "m")));

        assertEquals(3, project.getRubros().size());
        assertSame(root, project.getRubros().get(0)); assertSame(leaf, project.getRubros().get(1));
        RubroObra added = project.getRubros().get(2);
        assertSame(root, added.getParent());
        assertEquals("Vigas", added.getSubRubro()); assertEquals("Estructura", added.getRubro());
        assertEquals(5.0, added.getCantidad());
        assertEquals(List.of("1", "1.1", "1.2"), result.rubros().stream().map(item -> item.nRubro()).toList());
        assertEquals(10L, result.rubros().get(1).parentId());
        assertEquals(List.of(false, true, true), result.rubros().stream().map(item -> item.hoja()).toList());
    }

    @Test void updateRejectsForeignIdsAndUnknownParents() {
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest(
            row(10L, "1", null, "RUBRO", null, "Estructura", null, null), row(99L, "1.1", "1", "SUBRUBRO", null, "Ajena", 1.0, "m"))));
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest(
            row(10L, "1", null, "RUBRO", null, "Estructura", null, null), row(null, "1.2", "9", "SUBRUBRO", null, "Huérfana", 1.0, "m"))));
        assertEquals(List.of(root, leaf), project.getRubros());
        verify(projects, never()).save(any(Proyecto.class));
    }

    @Test void updateBlocksDeletingANodeWhoseChildrenRemain() {
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest(
            row(11L, "1.1", "1", "SUBRUBRO", null, "Losas", 10.0, "m2"))));

        // Same with a legacy payload (no kind, plain numeric codes): the parent row would be deleted under its child.
        root.setnRubro("5000"); leaf.setnRubro("5001");
        RubroCreateDto legacyChild = new RubroCreateDto("5001", "Estructura", null, null, "Losas", 10.0, "m2", null, null, "PROPIA", null,
            null, null, null, null, null, false, 11L, null, null);
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest(legacyChild)));
        assertEquals(List.of(root, leaf), project.getRubros());
        verify(projects, never()).save(any(Proyecto.class));
    }

    @Test void updateDeletesAWholeBranchWhenAllItsNodesAreOmitted() {
        var result = updateUseCase.execute(1L, updateRequest(row(null, "2", null, "RUBRO", null, "Otro", null, null)));
        assertEquals(1, project.getRubros().size());
        assertEquals("2", project.getRubros().get(0).getnRubro());
        assertEquals(1, result.rubros().size());
    }

    @Test void updateKeepsBlockingRemovalOfActivitiesWithTasksOrRestrictions() {
        taskOn(leaf);
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest()));

        when(tasks.findByProyectoId(1L)).thenReturn(List.of());
        when(restrictions.countByRubroVinculadoId(11L)).thenReturn(1L);
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest(
            row(10L, "1", null, "RUBRO", null, "Estructura", null, null))));
        assertTrue(project.getRubros().contains(leaf));
    }

    @Test void updateBlocksChangingTheVariantWhenAnActivityHasTasks() {
        taskOn(leaf);
        assertThrows(SolicitudInvalidaException.class, () -> updateUseCase.execute(1L, updateRequest(
            row(10L, "1", null, "RUBRO", "RUBRO_SUBRUBRO_NIVEL", "Estructura", null, null),
            row(11L, "1.1", "1", "SUBRUBRO", null, "Losas", 10.0, "m2"),
            row(null, "1.1.1", "1.1", "NIVEL", null, "P00", 4.0, "m2"))));
        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO, root.getVarianteEstructura());
        assertEquals(10.0, leaf.getCantidad());
    }

    @Test void updateChangesTheVariantOfARubroWithoutUsedActivities() {
        updateUseCase.execute(1L, updateRequest(
            row(10L, "1", null, "RUBRO", "RUBRO_SUBRUBRO_NIVEL", "Estructura", null, null),
            row(11L, "1.1", "1", "SUBRUBRO", null, "Losas", 10.0, "m2"),
            row(null, "1.1.1", "1.1", "NIVEL", null, "P00", 4.0, "m2")));

        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO_NIVEL, root.getVarianteEstructura());
        assertNull(leaf.getCantidad()); assertNull(leaf.getUnidad()); assertNull(leaf.getSubRubro());
        RubroObra nivel = byCode(project, "1.1.1");
        assertSame(leaf, nivel.getParent());
        assertEquals("P00", nivel.getSubRubro()); assertEquals("P00", nivel.getNivel());
        assertEquals(4.0, nivel.getCantidad());
    }

    @Test void updateKeepsLegacyTwoLayerPayloadsWorking() {
        root.setnRubro("5000"); leaf.setnRubro("5001");
        RubroCreateDto legacyRoot = new RubroCreateDto("5000", "Estructura", null, null, null, null, null, null, null, "PROPIA", null,
            null, null, null, null, null, false, 10L, null, null);
        RubroCreateDto legacyLeaf = new RubroCreateDto("5001", "Estructura", "N1", null, "Losas", 12.0, "m2", null, null, "PROPIA", null,
            null, null, null, null, null, false, 11L, null, null);

        updateUseCase.execute(1L, updateRequest(legacyRoot, legacyLeaf));

        assertSame(root, leaf.getParent());
        assertEquals(TipoNodoRubro.RUBRO, root.getTipoNodo());
        assertEquals(12.0, leaf.getCantidad()); assertEquals("N1", leaf.getNivel()); assertEquals("Losas", leaf.getSubRubro());
    }

    // ------------------------------------------------------------------ mapper

    @Test void mapperExposesStructureFieldsAndOrdersCodesNaturally() {
        Proyecto p = new Proyecto();
        p.setId(3L);
        RubroObra r = stored(p, 1L, "1", TipoNodoRubro.RUBRO, null, "Hormigón");
        r.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        RubroObra n = stored(p, 2L, "1.2", TipoNodoRubro.NIVEL, r, "Nivel");
        RubroObra s10 = stored(p, 3L, "1.2.10", TipoNodoRubro.SUBRUBRO, n, "Sub 10");
        RubroObra s2 = stored(p, 4L, "1.2.2", TipoNodoRubro.SUBRUBRO, n, "Sub 2");
        p.setRubros(List.of(s10, n, s2, r));

        var rubros = ProyectoMapper.toDetailDto(p).rubros();

        assertEquals(List.of("1", "1.2", "1.2.2", "1.2.10"), rubros.stream().map(item -> item.nRubro()).toList());
        assertNull(rubros.get(0).parentId());
        assertEquals(1L, rubros.get(1).parentId()); assertEquals(2L, rubros.get(2).parentId());
        assertEquals(List.of("RUBRO", "NIVEL", "SUBRUBRO", "SUBRUBRO"), rubros.stream().map(item -> item.tipoNodo()).toList());
        assertEquals("RUBRO_NIVEL_SUBRUBRO", rubros.get(0).varianteEstructura()); assertNull(rubros.get(1).varianteEstructura());
        assertEquals(List.of(false, false, true, true), rubros.stream().map(item -> item.hoja()).toList());
        assertEquals("Sub 2", rubros.get(2).nombre());
    }

    @Test void mapperKeepsTwoLayerLegacyRowsReadable() {
        Proyecto p = new Proyecto();
        p.setId(3L);
        RubroObra legacyRoot = new RubroObra(); legacyRoot.setId(1L); legacyRoot.setProyecto(p); legacyRoot.setnRubro("5000"); legacyRoot.setRubro("Hormigón");
        RubroObra legacyLeaf = new RubroObra(); legacyLeaf.setId(2L); legacyLeaf.setProyecto(p); legacyLeaf.setnRubro("5001");
        legacyLeaf.setRubro("Hormigón"); legacyLeaf.setSubRubro("Fundación");
        p.setRubros(List.of(legacyLeaf, legacyRoot));

        var rubros = ProyectoMapper.toDetailDto(p).rubros();

        assertEquals("RUBRO", rubros.get(0).tipoNodo()); assertEquals("RUBRO_SUBRUBRO", rubros.get(0).varianteEstructura());
        assertEquals("Hormigón", rubros.get(0).nombre()); assertFalse(rubros.get(0).hoja());
        assertEquals("SUBRUBRO", rubros.get(1).tipoNodo()); assertEquals("Fundación", rubros.get(1).nombre()); assertTrue(rubros.get(1).hoja());
    }

    // --------------------------------------------------------- leaf consumers

    private Proyecto threeLayerProject() {
        Proyecto p = new Proyecto();
        p.setId(1L);
        p.setEmpresa("Constructora");
        RubroObra r = stored(p, 1L, "1", TipoNodoRubro.RUBRO, null, "Hormigón");
        r.setRubro("Hormigón"); r.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO); r.setProductividad(99.0);
        RubroObra n = stored(p, 2L, "1.1", TipoNodoRubro.NIVEL, r, "General");
        n.setRubro("Hormigón");
        RubroObra s = stored(p, 3L, "1.1.1", TipoNodoRubro.SUBRUBRO, n, "Subrubro 1");
        s.setRubro("Hormigón"); s.setSubRubro("Subrubro 1"); s.setNivel("General"); s.setUnidad("m3");
        s.setCantidad(155.4); s.setHorasOficialesPrevistas(4000.); s.setHorasAyudantesPrevistas(1881.5); s.setProductividad(5.0);
        p.setRubros(List.of(r, n, s));
        return p;
    }

    @Test void previstoRealListsOnlyLeaves() {
        Proyecto p = threeLayerProject();
        var projectsMock = mock(ProyectoRepository.class);
        when(projectsMock.findById(1L)).thenReturn(Optional.of(p));
        var summary = new GetPrevistoRealUseCase(projectsMock, mock(ObraTareaRepository.class),
            mock(CategoriaManoObraEmpresaRepository.class), mock(CalendarioLaboralEmpresaRepository.class)).execute(1L);
        assertEquals(1, summary.actividades().size());
        assertEquals(3L, summary.actividades().get(0).rubroId());
        assertEquals("Subrubro 1", summary.actividades().get(0).nombre());
    }

    @Test void tasksCanOnlyBeLinkedToLeaves() {
        Proyecto p = threeLayerProject();
        var projectsMock = mock(ProyectoRepository.class);
        var tasksMock = mock(ObraTareaRepository.class);
        when(projectsMock.findById(1L)).thenReturn(Optional.of(p));
        when(tasksMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CreateObraTareaUseCase(tasksMock, projectsMock);
        for (long groupingId : new long[]{1L, 2L}) {
            var dto = new ObraTareaCreateDto("Carga", null, null, "kg", "x", null, null, null, null, 100., 80., null, 6., null, null,
                LocalDate.of(2026, 10, 14), groupingId);
            assertThrows(SolicitudInvalidaException.class, () -> useCase.execute(1L, dto));
        }
        verify(tasksMock, never()).save(any());
        var leafDto = new ObraTareaCreateDto("Carga", null, null, "kg", "x", null, null, null, null, 100., 80., null, 6., null, null,
            LocalDate.of(2026, 10, 14), 3L);
        var saved = useCase.execute(1L, leafDto);
        assertEquals(3L, saved.rubroId()); assertEquals("m3", saved.unidad()); assertEquals("General", saved.nivel());
    }

    @Test void restrictionsCanOnlyBeLinkedToLeaves() {
        Proyecto p = threeLayerProject();
        p.setNombre("Obra"); // the group rubro is given a subrubro text to prove the leaf check is structural
        p.getRubros().get(1).setSubRubro("General");
        var projectsMock = mock(ProyectoRepository.class);
        var rubrosMock = mock(RubroObraRepository.class);
        var restrictionsMock = mock(RestriccionObraRepository.class);
        when(projectsMock.findById(1L)).thenReturn(Optional.of(p));
        for (RubroObra rubro : p.getRubros()) when(rubrosMock.findById(rubro.getId())).thenReturn(Optional.of(rubro));
        when(restrictionsMock.save(any(RestriccionObra.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CreateRestriccionUseCase(restrictionsMock, projectsMock, rubrosMock);

        assertThrows(SolicitudInvalidaException.class, () -> useCase.execute(1L, restrictionRequest(2L)));
        verify(restrictionsMock, never()).save(any(RestriccionObra.class));
        var saved = useCase.execute(1L, restrictionRequest(3L));
        assertEquals("Hormigón · Subrubro 1", saved.rubro());
        assertEquals("General", saved.nivel());
    }

    private RestriccionCreateDto restrictionRequest(Long rubroId) {
        return new RestriccionCreateDto("Esperar inspección", rubroId, null, null, null, null, null,
            null, null, LocalDate.of(2026, 10, 10), null, null, null, null);
    }

    @Test void hourRecordsTakeTheProductivityOfTheLeafNotOfTheRubro() {
        Proyecto p = threeLayerProject();
        OffsetDateTime start = OffsetDateTime.parse("2026-10-29T08:00:00Z");
        var dto = new RegistroHorasCreateDto(null, "Hormigón", null, null, null, start, start.plusHours(1), null, null, null, null,
            null, null, null, null);
        RegistroHoras record = new RegistroHoras();
        RegistroHorasRules.apply(dto, record, p);
        assertEquals(5.0, record.getProductividadPresupuesto());
    }

    @Test void certificatesMeasureLeavesGroupedByTheRootRubro() {
        Proyecto p = threeLayerProject();
        var projectsMock = mock(ProyectoRepository.class);
        when(projectsMock.findById(1L)).thenReturn(Optional.of(p));
        var identity = mock(CurrentIdentity.class);
        when(identity.usuarioId()).thenReturn(5L);
        var users = mock(UsuarioEmpresaRepository.class);
        var user = mock(UsuarioEmpresa.class);
        when(user.getNombre()).thenReturn("Ana");
        when(users.findById(5L)).thenReturn(Optional.of(user));
        var useCase = new CertificadoObraUseCase(projectsMock, new CertificadoObraUseCaseTest.FakeCertificados(),
            new CertificadoObraUseCaseTest.FakeCompletados(), new CertificadoObraUseCaseTest.FakeTareas(), identity, users);

        assertEquals(1, useCase.empresas(1L).get(0).actividades());
        var detail = useCase.crear(1L, new CertificadoCrearDto(null, "MENSUAL", LocalDate.parse("2026-03-31"), 1));

        assertEquals(List.of(3L), detail.filas().stream().map(CertificadoFilaDto::rubroId).toList());
        assertEquals("1", detail.filas().get(0).grupoCodigo());
        assertEquals("Hormigón", detail.filas().get(0).grupoNombre());
        assertEquals("Subrubro 1", detail.filas().get(0).nombre());
    }
}
