package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PostgresMigrationSourceTest {
    private static final Pattern CREATE_TABLE = Pattern.compile(
        "(?is)CREATE TABLE\\s+([a-z][a-z0-9_]*)\\s*\\((.*?)\\);"
    );
    private static final Pattern COLUMN_NAME = Pattern.compile("^([a-z][a-z0-9_]*)\\s+.*$");
    private static final Map<String, Set<String>> EXPECTED_COLUMNS = Map.ofEntries(
        Map.entry("empresas", Set.of("id", "nombre", "identificacion_fiscal", "limite_usuarios", "creado_en", "creado_por_usuario_id")),
        Map.entry("planes_suscripcion", Set.of("id", "codigo", "nombre", "precio_mensual_centavos", "moneda", "dias_prueba", "nivel", "beneficios", "stripe_price_id", "activo")),
        Map.entry("usuarios_empresa", Set.of("id", "empresa_id", "email", "nombre", "password_hash", "rol", "activo", "super_admin")),
        Map.entry("proyectos", Set.of("id", "nombre", "descripcion", "presupuesto_usd", "direccion", "numero_proyecto", "fecha_inicio", "fecha_fin", "responsables_obra", "categorias_mano_obra", "cantidad_sectores", "empresa_id", "empresa", "horas_previstas", "horas_reales")),
        Map.entry("rendimientos", Set.of("id", "fase", "etapa", "sistema", "rol", "hh_porm2", "precio_por_hh")),
        Map.entry("gastos_fijos", Set.of("id", "concepto", "monto", "categoria", "mes")),
        Map.entry("calendarios_laborales_empresa", Set.of("id", "empresa_id", "horas_lunesaviernes", "horas_sabado", "trabaja_domingo", "paga_doble_feriado", "paga_doble_sabado", "paga_doble_no_laborable")),
        Map.entry("responsables_empresa", Set.of("id", "empresa_id", "rol", "nombre", "email", "telefono")),
        Map.entry("unidades_metricas_empresa", Set.of("id", "empresa_id", "codigo", "nombre")),
        Map.entry("catalogos_empresa", Set.of("id", "empresa_id", "tipo", "codigo", "nombre")),
        Map.entry("categorias_mano_obra_empresa", Set.of("id", "empresa_id", "nombre", "costo_hora", "habilitada")),
        Map.entry("plantas", Set.of("id", "proyecto_id", "nivel", "superficie", "sup_cubierta", "sup_descubierta", "sup_calculada", "coef_planta_tipo")),
        Map.entry("rubros_obra", Set.of("id", "proyecto_id", "n_rubro", "rubro", "nivel", "sub_numero_rubro", "sub_rubro", "cantidad", "unidad", "productividad", "empresa", "tipo_contratista", "subcontratista_id", "horas_oficiales_previstas", "horas_ayudantes_previstas", "rendimiento_oficial", "rendimiento_ayudante", "cantidad_personas", "fecha_inicio_planificada", "fecha_fin_planificada", "cronograma_manual")),
        Map.entry("registros_horas", Set.of("id", "proyecto_id", "numero_rubro", "rubro", "sub_rubro", "responsable", "etapa", "cantidad_prevista", "cantidad_real", "horas_previstas", "horas_oficiales", "horas_ayudantes", "productividad_presupuesto", "causa_no_cumplimiento", "medida_correctiva", "inicio", "fin", "horas", "descripcion")),
        Map.entry("presupuesto_lineas", Set.of("id", "proyecto_id", "etapa", "tipo_superficie", "funcion", "sistema", "m2", "hh_porm2", "precio_por_hh", "precio_por_hh_usd", "mes")),
        Map.entry("ingresos", Set.of("id", "proyecto_id", "concepto", "monto", "mes")),
        Map.entry("costos_indirectos", Set.of("id", "proyecto_id", "concepto", "monto", "mes")),
        Map.entry("restricciones_obra", Set.of("id", "proyecto_id", "restriccion", "tipo", "sector", "nivel", "unidad", "rubro", "responsable", "fecha_solicitud", "fecha_maxima_entrega", "fecha_real_entrega", "carga", "prioridad", "estado", "orden")),
        Map.entry("restriccion_comentarios", Set.of("id", "restriccion_id", "autor", "mensaje", "creado_en")),
        Map.entry("obra_tareas", Set.of("id", "proyecto_id", "rubro_id", "nombre", "sector", "nivel", "unidad", "rubro", "porcentaje_cumplimiento", "cumplimiento", "causa_no_cumplimiento", "comentario", "horas_oficiales", "horas_ayudantes", "cantidad_prevista", "cantidad_real", "medida_correctiva", "descripcion", "fecha")),
        Map.entry("entregas", Set.of("id", "proyecto_id", "nombre", "orden")),
        Map.entry("tareas", Set.of("id", "entrega_id", "fase", "etapa", "sistema", "nombre", "status", "avance_pct", "cantidad", "costo", "fecha_inicio", "fecha_fin", "mes")),
        Map.entry("suscripciones", Set.of("id", "titular", "usuario_id", "empresa_id", "plan_id", "stripe_customer_id", "stripe_subscription_id", "estado", "fin_prueba", "fin_periodo_actual", "cancela_al_final", "prueba_usada", "actualizado_en")),
        Map.entry("pagos_suscripcion", Set.of("id", "suscripcion_id", "stripe_invoice_id", "monto_centavos", "moneda", "estado", "fecha")),
        Map.entry("stripe_eventos_procesados", Set.of("event_id", "tipo", "procesado_en"))
    );

    @Test
    void initialMigrationContainsExactlyTheCurrentEntityTablesAndColumns() throws IOException {
        String migration = resource("db/migration/V1__initial_schema.sql");
        Matcher matcher = CREATE_TABLE.matcher(migration);
        Map<String, Set<String>> actual = new HashMap<>();

        while (matcher.find()) {
            Set<String> columns = new HashSet<>();
            for (String line : matcher.group(2).split("\\R")) {
                String definition = line.trim().replaceFirst(",$", "");
                if (definition.regionMatches(true, 0, "CONSTRAINT ", 0, "CONSTRAINT ".length())) continue;
                Matcher column = COLUMN_NAME.matcher(definition);
                if (column.matches()) columns.add(column.group(1));
            }
            actual.put(matcher.group(1), columns);
        }

        assertEquals(EXPECTED_COLUMNS.keySet(), actual.keySet());
        assertEquals(25, actual.size());
        EXPECTED_COLUMNS.forEach((table, columns) -> assertEquals(columns, actual.get(table), table));
        assertFalse(Pattern.compile("(?im)^\\s*INSERT\\s+INTO\\s+").matcher(migration).find());
        assertTrue(migration.contains("GENERATED BY DEFAULT AS IDENTITY"));
        assertTrue(migration.contains("TIMESTAMP WITH TIME ZONE"));
        assertTrue(migration.contains("TIMESTAMP WITHOUT TIME ZONE"));
        assertTrue(migration.contains("FOREIGN KEY (proyecto_id) REFERENCES proyectos (id)"));
        assertTrue(migration.contains("responsables_obra VARCHAR(1000)"));
        assertTrue(migration.contains("categorias_mano_obra VARCHAR(1000)"));
        assertTrue(migration.contains("cantidad_sectores INTEGER DEFAULT 1"));
        assertTrue(migration.contains("medida_correctiva VARCHAR(2000)"));
        assertTrue(migration.contains("descripcion VARCHAR(2000)"));
    }

    @Test
    void secondMigrationSeedsOnlyApprovedPlansIdempotentlyWithoutStripeIds() throws IOException {
        String migration = resource("db/migration/V2__seed_plan_catalog.sql");

        assertTrue(migration.contains("INSERT INTO planes_suscripcion"));
        assertTrue(migration.contains("'BASIC', 'Basic', 900, 'usd'"));
        assertTrue(migration.contains("'PRO', 'Pro', 1900, 'usd'"));
        assertTrue(migration.contains("ON CONFLICT (codigo) DO NOTHING"));
        assertTrue(migration.contains("NULL, TRUE"));
        assertFalse(migration.toLowerCase(java.util.Locale.ROOT).contains("stripe_customer"));
    }

    private String resource(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, "Missing migration resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
