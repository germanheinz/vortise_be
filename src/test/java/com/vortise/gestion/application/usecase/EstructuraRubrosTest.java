package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.model.TipoNodoRubro;
import com.vortise.gestion.domain.model.VarianteEstructura;
import com.vortise.gestion.domain.service.EstructuraRubros;
import com.vortise.gestion.domain.service.EstructuraRubros.Entrada;
import com.vortise.gestion.domain.service.EstructuraRubros.Nodo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure domain rules of the 3-layer structure: no Spring, no database. */
class EstructuraRubrosTest {
    private static Entrada raiz(String codigo, String nombre, String variante) {
        return new Entrada(codigo, null, "RUBRO", variante, nombre, null, null, null, null, null);
    }

    private static Entrada nodo(String codigo, String padre, String tipo, String nombre) {
        return new Entrada(codigo, padre, tipo, null, nombre, null, null, null, null, null);
    }

    private static Entrada legado(String codigo, String rubro, String subRubro) {
        return new Entrada(codigo, null, null, null, null, rubro, subRubro, null, null, null);
    }

    private static List<Nodo> resolver(Entrada... entradas) {
        return EstructuraRubros.resolver(Arrays.asList(entradas));
    }

    private static void assertInvalida(Entrada... entradas) {
        assertThrows(SolicitudInvalidaException.class, () -> resolver(entradas));
    }

    // ------------------------------------------------------------ variants

    @Test void rubroSubrubroHasOneLeafLayerAndKeepsTheSentNivel() {
        var sub = new Entrada("1.1", "1", "SUBRUBRO", null, "Losas", null, null, "Nivel 2", null, null);
        var nodos = resolver(raiz("1", "Estructura", null), sub);

        assertEquals(TipoNodoRubro.RUBRO, nodos.get(0).tipo());
        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO, nodos.get(0).variante());
        assertFalse(nodos.get(0).hoja());
        assertEquals("Estructura", nodos.get(0).rubro());
        assertNull(nodos.get(0).subRubro()); assertNull(nodos.get(0).nivel());

        assertTrue(nodos.get(1).hoja());
        assertEquals(2, nodos.get(1).capa());
        assertEquals("1", nodos.get(1).parentCodigo());
        assertEquals("Estructura", nodos.get(1).rubro());
        assertEquals("Losas", nodos.get(1).subRubro());
        assertEquals("Nivel 2", nodos.get(1).nivel());
        assertNull(nodos.get(1).variante());
    }

    @Test void rubroNivelSubrubroDerivesNivelFromTheLevelNode() {
        var nodos = resolver(
            raiz("1", "Hormigón", "RUBRO_NIVEL_SUBRUBRO"),
            nodo("1.1", "1", "NIVEL", "General de la obra"),
            nodo("1.1.1", "1.1", "SUBRUBRO", "Subrubro 1"),
            nodo("1.1.2", "1.1", "SUBRUBRO", "Subrubro 2"));

        assertEquals(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO, nodos.get(0).variante());
        assertFalse(nodos.get(1).hoja());
        assertEquals("Hormigón", nodos.get(1).rubro());
        assertNull(nodos.get(1).subRubro()); assertNull(nodos.get(1).nivel());
        for (int i = 2; i <= 3; i++) {
            assertTrue(nodos.get(i).hoja());
            assertEquals(3, nodos.get(i).capa());
            assertEquals("Hormigón", nodos.get(i).rubro());
            assertEquals("General de la obra", nodos.get(i).nivel());
        }
        assertEquals("Subrubro 1", nodos.get(2).subRubro());
        assertEquals("Subrubro 2", nodos.get(3).subRubro());
    }

    @Test void rubroSubrubroNivelHasTheNivelAsLeaf() {
        var nodos = resolver(
            raiz("3", "Terminaciones", "RUBRO_SUBRUBRO_NIVEL"),
            nodo("3.1", "3", "SUBRUBRO", "Subrubro"),
            nodo("3.1.1", "3.1", "NIVEL", "Subsuelo"),
            nodo("3.1.2", "3.1", "NIVEL", "P00"));

        assertFalse(nodos.get(1).hoja());
        assertNull(nodos.get(1).subRubro());
        assertTrue(nodos.get(2).hoja());
        assertEquals("Subsuelo", nodos.get(2).subRubro());
        assertEquals("Subsuelo", nodos.get(2).nivel());
        assertEquals("Terminaciones", nodos.get(2).rubro());
        assertEquals("P00", nodos.get(3).nivel());
    }

    @Test void variantsCanBeMixedAndNumberingMayHaveGaps() {
        var nodos = resolver(
            raiz("2", "Estructura", null),
            nodo("2.5", "2", "SUBRUBRO", "Vigas"),
            raiz("7", "Terminaciones", "RUBRO_NIVEL_SUBRUBRO"),
            nodo("7.3", "7", "NIVEL", "PB"),
            nodo("7.3.9", "7.3", "SUBRUBRO", "Pintura"));

        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO, nodos.get(0).variante());
        assertTrue(nodos.get(1).hoja());
        assertEquals(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO, nodos.get(2).variante());
        assertTrue(nodos.get(4).hoja());
        assertEquals("PB", nodos.get(4).nivel());
    }

    @Test void rootWithoutChildrenIsAnEmptyGroupingNode() {
        var nodos = resolver(raiz("1", "Vacío", null));
        assertFalse(nodos.get(0).hoja());
    }

    @Test void levelWithoutChildrenIsNotAnActivity() {
        var nodos = resolver(raiz("1", "Hormigón", "RUBRO_NIVEL_SUBRUBRO"), nodo("1.1", "1", "NIVEL", "Sin subrubros"));
        assertFalse(nodos.get(1).hoja());
    }

    @Test void missingVariantDefaultsToRubroSubrubroAndNonRootVariantIsIgnored() {
        var conVariante = new Entrada("1.1", "1", "SUBRUBRO", "RUBRO_NIVEL_SUBRUBRO", "Losas", null, null, null, null, null);
        var nodos = resolver(raiz("1", "Estructura", " "), conVariante);
        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO, nodos.get(0).variante());
        assertNull(nodos.get(1).variante());
    }

    // ---------------------------------------------------------- validation

    @Test void rejectsMoreThanThreeLayersAndLayersBeyondTheVariant() {
        assertInvalida(raiz("1", "A", null), nodo("1.1", "1", "SUBRUBRO", "B"), nodo("1.1.1", "1.1", "SUBRUBRO", "C"));
        assertInvalida(raiz("1", "A", "RUBRO_NIVEL_SUBRUBRO"), nodo("1.1", "1", "NIVEL", "B"),
            nodo("1.1.1", "1.1", "SUBRUBRO", "C"), nodo("1.1.1.1", "1.1.1", "SUBRUBRO", "D"));
    }

    @Test void rejectsKindsThatDoNotMatchTheLayerOfTheVariant() {
        assertInvalida(raiz("1", "A", "RUBRO_NIVEL_SUBRUBRO"), nodo("1.1", "1", "SUBRUBRO", "B"));
        assertInvalida(raiz("1", "A", "RUBRO_SUBRUBRO_NIVEL"), nodo("1.1", "1", "NIVEL", "B"));
        assertInvalida(raiz("1", "A", null), nodo("1.1", "1", "NIVEL", "B"));
        assertInvalida(raiz("1", "A", null), nodo("1.1", "1", "RUBRO", "B"));
    }

    @Test void rejectsDuplicateCodes() {
        assertInvalida(raiz("1", "A", null), raiz("1", "B", null));
        assertInvalida(raiz("1", "A", null), nodo("1.1", "1", "SUBRUBRO", "B"), nodo("1.1", "1", "SUBRUBRO", "C"));
    }

    @Test void rejectsUnknownMissingOrSelfParents() {
        assertInvalida(raiz("1", "A", null), nodo("1.1", "9", "SUBRUBRO", "B"));
        assertInvalida(raiz("1", "A", null), nodo("1.1", null, "SUBRUBRO", "B"));
        assertInvalida(raiz("1", "A", null), nodo("1.1", "1.1", "SUBRUBRO", "B"));
        assertInvalida(new Entrada("1", "2", "RUBRO", null, "A", null, null, null, null, null), raiz("2", "B", null));
    }

    @Test void rejectsCodesInconsistentWithTheirParent() {
        assertInvalida(raiz("1", "A", null), raiz("2", "B", null), nodo("2.1", "1", "SUBRUBRO", "C"));
        assertInvalida(raiz("1", "A", "RUBRO_NIVEL_SUBRUBRO"), nodo("1.1", "1", "NIVEL", "B"), nodo("1.1.1", "1", "SUBRUBRO", "C"));
        assertInvalida(raiz("1", "A", "RUBRO_NIVEL_SUBRUBRO"), nodo("1.1", "1", "NIVEL", "B"), nodo("1.2.1", "1.1", "SUBRUBRO", "C"));
        assertInvalida(raiz("1", "A", null), nodo("11", "1", "SUBRUBRO", "B"));
    }

    @Test void rejectsMalformedCodes() {
        for (String codigo : new String[]{null, " ", "a", "1.a", "1.", ".1", "1..1", "0", "01", "1.0", "1.01", "1.1.1.1", "-1"}) {
            assertInvalida(raiz(codigo, "A", null));
        }
        assertInvalida(raiz("1.1", "A", null));
    }

    @Test void rejectsMissingNamesUnknownKindsAndVariants() {
        assertInvalida(raiz("1", null, null));
        assertInvalida(raiz("1", "  ", null));
        assertInvalida(raiz("1", "x".repeat(256), null));
        assertInvalida(new Entrada("1", null, "CAPITULO", null, "A", null, null, null, null, null));
        assertInvalida(raiz("1", "A", "RUBRO_RUBRO"));
    }

    @Test void acceptsCaseInsensitiveKindsAndTrimsCodes() {
        var nodos = EstructuraRubros.resolver(List.of(
            new Entrada(" 1 ", null, "rubro", "rubro_subrubro", " Estructura ", null, null, null, null, null),
            new Entrada("1.1", " 1 ", "subrubro", null, "Losas", null, null, null, null, null)));
        assertEquals("1", nodos.get(0).codigo());
        assertEquals("Estructura", nodos.get(0).nombre());
        assertEquals("1", nodos.get(1).parentCodigo());
    }

    // ------------------------------------------------- legacy 2-layer payloads

    @Test void legacyNumericCodesAreInferredLikeBefore() {
        var nodos = resolver(legado("5000", "Hormigón", null), legado("5001", "Hormigón", "Fundación"), legado("6001", "Hierro", null));

        assertEquals(TipoNodoRubro.RUBRO, nodos.get(0).tipo());
        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO, nodos.get(0).variante());
        assertFalse(nodos.get(0).hoja());
        assertEquals("Hormigón", nodos.get(0).nombre());

        assertEquals(TipoNodoRubro.SUBRUBRO, nodos.get(1).tipo());
        assertEquals("5000", nodos.get(1).parentCodigo());
        assertTrue(nodos.get(1).hoja());
        assertEquals("Fundación", nodos.get(1).nombre());
        assertEquals("Hormigón", nodos.get(1).rubro());
        assertEquals("Fundación", nodos.get(1).subRubro());

        // No root 6000 in the list: accepted as an orphan leaf, legacy columns as sent.
        assertEquals(TipoNodoRubro.SUBRUBRO, nodos.get(2).tipo());
        assertNull(nodos.get(2).parentCodigo());
        assertTrue(nodos.get(2).hoja());
        assertEquals("Hierro", nodos.get(2).rubro());
        assertNull(nodos.get(2).subRubro());
        assertEquals("Hierro", nodos.get(2).nombre());
    }

    @Test void legacyNonHierarchicalOrBlankCodesAreAcceptedAsTheyCome() {
        var nodos = resolver(legado("ABC", "Rubro", "Sub"), legado(null, "Otro", "Sub"), legado("", "Otro", "Sub 2"), legado("12", "Otro", "Sub 3"));
        for (Nodo nodo : nodos) {
            assertEquals(TipoNodoRubro.SUBRUBRO, nodo.tipo());
            assertTrue(nodo.hoja());
            assertNull(nodo.parentCodigo());
        }
    }

    @Test void legacyDuplicateCodesAreStillRejected() {
        assertInvalida(legado("5001", "A", "x"), legado("5001", "A", "y"));
    }

    @Test void legacyKindAndVariantFollowTheStoredRowWhenThePayloadHasNoKind() {
        var raizExistente = new Entrada("1", null, null, null, null, "Hormigón", null, null, TipoNodoRubro.RUBRO, VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        var nivel = new Entrada("1.1", null, null, null, null, "Hormigón", "General", null, null, null);
        var sub = new Entrada("1.1.1", null, null, null, null, "Hormigón", "Subrubro 1", "General", null, null);

        var nodos = resolver(raizExistente, nivel, sub);

        assertEquals(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO, nodos.get(0).variante());
        assertEquals(TipoNodoRubro.NIVEL, nodos.get(1).tipo());
        assertFalse(nodos.get(1).hoja());
        assertEquals(TipoNodoRubro.SUBRUBRO, nodos.get(2).tipo());
        assertEquals("1.1", nodos.get(2).parentCodigo());
        assertTrue(nodos.get(2).hoja());
    }

    @Test void legacyDottedChildWithoutItsParentIsRejected() {
        assertInvalida(legado("1.1", "A", "B"));
    }

    // ------------------------------------------------------------- code order

    @Test void codesAreOrderedNaturallySegmentBySegment() {
        List<String> codes = new ArrayList<>(Arrays.asList("1.10", "1.2", "2", null, "1", "x", "1.1.1", "10", "5001", "5000"));
        codes.sort(EstructuraRubros.ORDEN_CODIGO);
        assertEquals(Arrays.asList("1", "1.1.1", "1.2", "1.10", "2", "10", "5000", "5001", "x", null), codes);
    }

    @Test void rubrosAreOrderedByNaturalCode() {
        List<RubroObra> rubros = new ArrayList<>(List.of(rubro(1L, "2", TipoNodoRubro.RUBRO, null), rubro(2L, "1.10", TipoNodoRubro.SUBRUBRO, null),
            rubro(3L, "1.2", TipoNodoRubro.SUBRUBRO, null), rubro(4L, "1", TipoNodoRubro.RUBRO, null)));
        rubros.sort(EstructuraRubros.ORDEN_NATURAL);
        assertEquals(List.of("1", "1.2", "1.10", "2"), rubros.stream().map(RubroObra::getnRubro).toList());
    }

    // ------------------------------------------------------- stored structure

    private static RubroObra rubro(Long id, String codigo, TipoNodoRubro tipo, RubroObra padre) {
        RubroObra rubro = new RubroObra();
        rubro.setId(id);
        rubro.setnRubro(codigo);
        rubro.setTipoNodo(tipo);
        rubro.setParent(padre);
        return rubro;
    }

    @Test void leavesParentsAndRootFollowTheParentLinksOfThreeLayers() {
        var root = rubro(1L, "1", TipoNodoRubro.RUBRO, null);
        root.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        var level = rubro(2L, "1.1", TipoNodoRubro.NIVEL, root);
        var leaf = rubro(3L, "1.1.1", TipoNodoRubro.SUBRUBRO, level);
        var emptyLevel = rubro(4L, "1.2", TipoNodoRubro.NIVEL, root);
        var all = List.of(root, level, leaf, emptyLevel);

        assertEquals(List.of(leaf), EstructuraRubros.hojas(all));
        assertFalse(EstructuraRubros.esHoja(root, all));
        assertFalse(EstructuraRubros.esHoja(level, all));
        assertFalse(EstructuraRubros.esHoja(emptyLevel, all));
        assertTrue(EstructuraRubros.tieneHijos(root, all));
        assertFalse(EstructuraRubros.tieneHijos(leaf, all));
        assertEquals(3, EstructuraRubros.capa(leaf));
        assertSame(root, EstructuraRubros.raiz(leaf, all));
        assertSame(root, EstructuraRubros.raiz(root, all));
    }

    @Test void nivelAsLeafInVariantThree() {
        var root = rubro(1L, "3", TipoNodoRubro.RUBRO, null);
        root.setVarianteEstructura(VarianteEstructura.RUBRO_SUBRUBRO_NIVEL);
        var sub = rubro(2L, "3.1", TipoNodoRubro.SUBRUBRO, root);
        var nivel = rubro(3L, "3.1.1", TipoNodoRubro.NIVEL, sub);
        var all = List.of(root, sub, nivel);

        assertEquals(List.of(nivel), EstructuraRubros.hojas(all));
        assertFalse(EstructuraRubros.esHoja(sub, all));
    }

    @Test void parentsAreMatchedByIdWhenTheInstanceDiffers() {
        var root = rubro(1L, "1", TipoNodoRubro.RUBRO, null);
        var sameRowOtherInstance = rubro(1L, "1", TipoNodoRubro.RUBRO, null);
        var leaf = rubro(2L, "1.1", TipoNodoRubro.SUBRUBRO, sameRowOtherInstance);
        assertTrue(EstructuraRubros.tieneHijos(root, List.of(root, leaf)));
    }

    @Test void legacyRowsWithoutParentLinkInferKindAndRootFromTheNumericCode() {
        var root = rubro(1L, "5000", null, null);
        var leaf = rubro(2L, "5001", null, null);
        var orphan = rubro(3L, "6001", null, null);
        var noCode = rubro(4L, null, null, null);
        var all = List.of(root, leaf, orphan, noCode);

        assertEquals(TipoNodoRubro.RUBRO, root.getTipoNodo());
        assertEquals(VarianteEstructura.RUBRO_SUBRUBRO, root.getVarianteEstructura());
        assertEquals(TipoNodoRubro.SUBRUBRO, leaf.getTipoNodo());
        assertEquals(List.of(leaf, orphan, noCode), EstructuraRubros.hojas(all));
        assertSame(root, EstructuraRubros.raiz(leaf, all));
        assertNull(EstructuraRubros.raiz(orphan, all));
        assertNull(EstructuraRubros.raiz(noCode, all));
    }

    @Test void nameFallsBackToLegacyColumns() {
        var root = rubro(1L, "5000", null, null);
        root.setRubro("Hormigón");
        var leaf = rubro(2L, "5001", null, null);
        leaf.setRubro("Hormigón"); leaf.setSubRubro("Fundación");
        var onlyRubro = rubro(3L, "6001", null, null);
        onlyRubro.setRubro("Hierro");
        assertEquals("Hormigón", EstructuraRubros.nombreDe(root));
        assertEquals("Fundación", EstructuraRubros.nombreDe(leaf));
        assertEquals("Hierro", EstructuraRubros.nombreDe(onlyRubro));
        leaf.setNombre("Explícito");
        assertEquals("Explícito", EstructuraRubros.nombreDe(leaf));
    }
}
