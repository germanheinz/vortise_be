package com.vortise.gestion.infrastructure.backup;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!staging & !prod & !production & !e2e-local")
public class H2LogicalBackupService {
    private static final int FORMAT_VERSION = 1;
    public static final int MAX_BACKUP_BYTES = 50 * 1024 * 1024;

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final ObjectMapper objectMapper;

    public H2LogicalBackupService(JdbcTemplate jdbc, DataSource dataSource,
            EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public byte[] exportBackup() {
        try (Connection connection = dataSource.getConnection()) {
            requireH2(connection.getMetaData());
            List<TableDefinition> definitions = discoverTables(connection);
            ObjectNode backup = objectMapper.createObjectNode();
            backup.put("version", FORMAT_VERSION);
            backup.put("schemaFingerprint", fingerprint(definitions));
            ArrayNode tables = backup.putArray("tables");
            for (TableDefinition definition : definitions) {
                ObjectNode table = writeDefinition(definition);
                ArrayNode rows = table.putArray("rows");
                String select = "SELECT " + definition.columns().stream().map(c -> quote(connection, c.name()))
                    .reduce((a, b) -> a + "," + b).orElseThrow()
                    + " FROM " + quote(connection, definition.name());
                jdbc.query(select, resultSet -> {
                    while (resultSet.next()) {
                        ArrayNode row = rows.addArray();
                        for (int i = 0; i < definition.columns().size(); i++) {
                            ColumnDefinition column = definition.columns().get(i);
                            Object value = resultSet.getObject(i + 1);
                            if (value == null) {
                                row.addNull();
                            } else if (isBinary(column.jdbcType())) {
                                row.add(Base64.getEncoder().encodeToString(resultSet.getBytes(i + 1)));
                            } else if (isCharacter(column.jdbcType())) {
                                row.add(resultSet.getString(i + 1));
                            } else if (isTemporal(column.jdbcType())) {
                                row.add(value.toString());
                            } else {
                                row.add(resultSet.getString(i + 1));
                            }
                        }
                    }
                    return null;
                });
                tables.add(table);
            }
            byte[] content = objectMapper.writeValueAsBytes(backup);
            if (content.length > MAX_BACKUP_BYTES) {
                throw new IllegalStateException("El respaldo supera el máximo de 50 MiB compatible con la restauración");
            }
            return content;
        } catch (SQLException | JacksonException ex) {
            throw new IllegalStateException("No se pudo generar el respaldo lógico", ex);
        }
    }

    @Transactional
    public RestoreSummary restore(byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_BACKUP_BYTES) {
            throw new BackupValidationException("El archivo está vacío o supera el máximo de 50 MiB");
        }
        final JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (JacksonException ex) {
            throw new BackupValidationException("El archivo no contiene JSON válido", ex);
        }
        try (Connection connection = dataSource.getConnection()) {
            requireH2(connection.getMetaData());
            List<TableDefinition> current = discoverTables(connection);
            ParsedBackup parsed = validateBackup(root, current);
            Map<Long, Long> previousVersions = jdbc.query("SELECT id, session_version FROM usuarios_empresa", rs -> {
                Map<Long, Long> versions = new HashMap<>();
                while (rs.next()) versions.put(rs.getLong(1), rs.getLong(2));
                return versions;
            });
            List<TableDefinition> insertOrder = dependencyOrder(current);
            List<IdentityRestart> restarts = identityRestarts(parsed.tables(), insertOrder);

            // Restoring an older snapshot must not resurrect recovery links or sessions.
            jdbc.update("DELETE FROM password_reset_tokens");
            for (int i = insertOrder.size() - 1; i >= 0; i--) {
                jdbc.update("DELETE FROM " + quote(connection, insertOrder.get(i).name()));
            }
            for (TableDefinition definition : insertOrder) {
                ParsedTable table = parsed.tables().get(definition.name());
                insertRows(connection, definition, table.rows());
            }
            for (IdentityRestart restart : restarts) {
                jdbc.execute("ALTER TABLE " + quote(connection, restart.table()) + " ALTER COLUMN "
                    + quote(connection, restart.column()) + " RESTART WITH " + restart.nextValue());
            }
            jdbc.query("SELECT id, session_version FROM usuarios_empresa", rs -> {
                long id = rs.getLong(1);
                // Restores can remove/reintroduce an account: a counter based only on current
                // rows may repeat a generation previously signed into a still-valid JWT.
                long restored = rs.getLong(2);
                long previous = previousVersions.getOrDefault(id, 0L);
                long version;
                do { version = com.vortise.gestion.domain.model.UsuarioEmpresa.nuevaGeneracionSesion(); }
                while (version == restored || version == previous);
                jdbc.update("UPDATE usuarios_empresa SET session_version = ? WHERE id = ?", version, id);
            });
            return new RestoreSummary(current.size(), parsed.rowCount());
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo restaurar el respaldo lógico", ex);
        }
    }

    private ParsedBackup validateBackup(JsonNode root, List<TableDefinition> current) {
        requireFields(root, Set.of("version", "schemaFingerprint", "tables"), "raíz");
        if (!root.path("version").isIntegralNumber() || root.path("version").intValue() != FORMAT_VERSION) {
            throw new BackupValidationException("Versión de respaldo no compatible");
        }
        if (!root.path("schemaFingerprint").isTextual()
                || !fingerprint(current).equals(root.path("schemaFingerprint").textValue())) {
            throw new BackupValidationException("El esquema del respaldo no coincide con la base de destino");
        }
        JsonNode tablesNode = root.get("tables");
        if (tablesNode == null || !tablesNode.isArray() || tablesNode.size() != current.size()) {
            throw new BackupValidationException("El conjunto de tablas del respaldo no es válido");
        }
        Map<String, TableDefinition> expected = new TreeMap<>();
        current.forEach(table -> expected.put(table.name(), table));
        Map<String, ParsedTable> parsed = new LinkedHashMap<>();
        long rowCount = 0;
        for (JsonNode tableNode : tablesNode) {
            requireFields(tableNode, Set.of("name", "columns", "primaryKey", "uniqueKeys", "foreignKeys", "rows"), "tabla");
            String name = requiredText(tableNode, "name");
            TableDefinition definition = expected.get(name);
            if (definition == null || parsed.containsKey(name)) {
                throw new BackupValidationException("El respaldo contiene una tabla desconocida o repetida");
            }
            validateDefinition(tableNode, definition);
            JsonNode rowsNode = tableNode.get("rows");
            if (rowsNode == null || !rowsNode.isArray()) {
                throw new BackupValidationException("Las filas de la tabla " + name + " no son válidas");
            }
            List<ParsedRow> rows = new ArrayList<>(rowsNode.size());
            for (JsonNode rowNode : rowsNode) {
                if (!rowNode.isArray() || rowNode.size() != definition.columns().size()) {
                    throw new BackupValidationException("Una fila de " + name + " no coincide con sus columnas");
                }
                List<Object> values = new ArrayList<>(rowNode.size());
                for (int i = 0; i < rowNode.size(); i++) {
                    JsonNode cell = rowNode.get(i);
                    ColumnDefinition column = definition.columns().get(i);
                    if (cell == null || cell.isNull()) {
                        if (!column.nullable()) throw new BackupValidationException("Valor nulo en columna obligatoria " + name + "." + column.name());
                        values.add(null);
                    } else {
                        if (!cell.isTextual()) throw new BackupValidationException("Tipo de valor inválido en " + name + "." + column.name());
                        values.add(parseValue(cell.textValue(), column));
                    }
                }
                rows.add(new ParsedRow(values));
            }
            rowCount += rows.size();
            parsed.put(name, new ParsedTable(definition, rows));
        }
        if (parsed.size() != current.size()) throw new BackupValidationException("Faltan tablas en el respaldo");
        validateConstraints(parsed);
        return new ParsedBackup(parsed, rowCount);
    }

    private void validateDefinition(JsonNode tableNode, TableDefinition expected) {
        if (!requiredArray(tableNode, "columns").equals(writeColumns(expected.columns()))) {
            throw new BackupValidationException("Las columnas o tipos de " + expected.name() + " no coinciden");
        }
        if (!tableNode.get("primaryKey").equals(writeStrings(expected.primaryKey()))) {
            throw new BackupValidationException("La clave primaria de " + expected.name() + " no coincide");
        }
        if (!tableNode.get("uniqueKeys").equals(writeStringGroups(expected.uniqueKeys()))) {
            throw new BackupValidationException("Las restricciones únicas de " + expected.name() + " no coinciden");
        }
        if (!tableNode.get("foreignKeys").equals(writeForeignKeys(expected.foreignKeys()))) {
            throw new BackupValidationException("Las relaciones de " + expected.name() + " no coinciden");
        }
    }

    private void validateConstraints(Map<String, ParsedTable> tables) {
        Map<String, Map<List<String>, Set<List<String>>>> indexes = new HashMap<>();
        for (ParsedTable table : tables.values()) {
            List<List<String>> keys = new ArrayList<>();
            if (!table.definition().primaryKey().isEmpty()) keys.add(table.definition().primaryKey());
            keys.addAll(table.definition().uniqueKeys());
            for (List<String> columns : keys) {
                Set<List<String>> values = new HashSet<>();
                for (ParsedRow row : table.rows()) {
                    List<String> key = canonicalKey(table.definition(), row, columns);
                    if (key.contains(null) && !columns.equals(table.definition().primaryKey())) continue;
                    if (!values.add(key)) throw new BackupValidationException("La tabla " + table.definition().name() + " contiene claves repetidas");
                }
                indexes.computeIfAbsent(table.definition().name(), ignored -> new HashMap<>()).put(columns, values);
            }
        }
        for (ParsedTable table : tables.values()) {
            for (ForeignKey foreignKey : table.definition().foreignKeys()) {
                ParsedTable referenced = tables.get(foreignKey.referencedTable());
                Set<List<String>> referencedValues = indexes.computeIfAbsent(referenced.definition().name(), ignored -> new HashMap<>())
                    .computeIfAbsent(foreignKey.referencedColumns(), columns -> {
                        Set<List<String>> values = new HashSet<>();
                        for (ParsedRow row : referenced.rows()) values.add(canonicalKey(referenced.definition(), row, columns));
                        return values;
                    });
                for (ParsedRow row : table.rows()) {
                    List<String> key = canonicalKey(table.definition(), row, foreignKey.columns());
                    if (!key.contains(null) && !referencedValues.contains(key)) {
                        throw new BackupValidationException("La tabla " + table.definition().name() + " contiene una referencia inexistente");
                    }
                }
            }
        }
    }

    private List<String> canonicalKey(TableDefinition definition, ParsedRow row, List<String> columns) {
        List<String> key = new ArrayList<>(columns.size());
        for (String name : columns) {
            int index = columnIndex(definition, name);
            Object value = row.values().get(index);
            if (value instanceof BigDecimal decimal) key.add(decimal.stripTrailingZeros().toPlainString());
            else if (value instanceof byte[] bytes) key.add(Base64.getEncoder().encodeToString(bytes));
            else key.add(value == null ? null : value.toString());
        }
        return key;
    }

    private List<IdentityRestart> identityRestarts(Map<String, ParsedTable> tables, List<TableDefinition> order) {
        List<IdentityRestart> restarts = new ArrayList<>();
        for (TableDefinition definition : order) {
            List<ColumnDefinition> identities = definition.columns().stream().filter(column -> column.autoIncrement()).toList();
            if (identities.size() > 1) throw new BackupValidationException("Identity múltiple no soportada en " + definition.name());
            if (identities.isEmpty()) continue;
            ColumnDefinition identity = identities.get(0);
            BigInteger max = null;
            for (ParsedRow row : tables.get(definition.name()).rows()) {
                Object value = row.values().get(columnIndex(definition, identity.name()));
                if (value != null) {
                    BigInteger current = new BigDecimal(value.toString()).toBigIntegerExact();
                    if (max == null || current.compareTo(max) > 0) max = current;
                }
            }
            BigInteger next = max == null ? BigInteger.ONE : max.add(BigInteger.ONE);
            if (next.signum() <= 0 || next.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new BackupValidationException("El valor identity de " + definition.name() + " está fuera de rango");
            }
            restarts.add(new IdentityRestart(definition.name(), identity.name(), next));
        }
        return restarts;
    }

    private void insertRows(Connection connection, TableDefinition definition, List<ParsedRow> rows) {
        if (rows.isEmpty()) return;
        String columns = definition.columns().stream().map(c -> quote(connection, c.name())).reduce((a, b) -> a + "," + b).orElseThrow();
        String placeholders = String.join(",", java.util.Collections.nCopies(definition.columns().size(), "?"));
        String sql = "INSERT INTO " + quote(connection, definition.name()) + " (" + columns + ") VALUES (" + placeholders + ")";
        for (ParsedRow row : rows) {
            jdbc.update(sql, statement -> {
                for (int i = 0; i < row.values().size(); i++) {
                    Object value = row.values().get(i);
                    if (value == null) statement.setNull(i + 1, definition.columns().get(i).jdbcType());
                    else statement.setObject(i + 1, value, definition.columns().get(i).jdbcType());
                }
            });
        }
    }

    private Object parseValue(String value, ColumnDefinition column) {
        try {
            return switch (column.jdbcType()) {
                case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR,
                        Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> value;
                case Types.TINYINT -> exactInteger(value, Byte.MIN_VALUE, Byte.MAX_VALUE).byteValueExact();
                case Types.SMALLINT -> exactInteger(value, Short.MIN_VALUE, Short.MAX_VALUE).shortValueExact();
                case Types.INTEGER -> exactInteger(value, Integer.MIN_VALUE, Integer.MAX_VALUE).intValueExact();
                case Types.BIGINT -> exactInteger(value, Long.MIN_VALUE, Long.MAX_VALUE).longValueExact();
                case Types.NUMERIC, Types.DECIMAL -> parseDecimal(value, column);
                case Types.FLOAT, Types.REAL -> finiteFloat(value);
                case Types.DOUBLE -> finiteDouble(value);
                case Types.BOOLEAN, Types.BIT -> parseBoolean(value);
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Base64.getDecoder().decode(value);
                case Types.DATE -> Date.valueOf(LocalDate.parse(value));
                case Types.TIME -> Time.valueOf(LocalTime.parse(value));
                case Types.TIMESTAMP -> Timestamp.valueOf(value.replace('T', ' '));
                case Types.TIME_WITH_TIMEZONE -> OffsetTime.parse(value);
                case Types.TIMESTAMP_WITH_TIMEZONE -> OffsetDateTime.parse(value);
                default -> throw new BackupValidationException("Tipo JDBC no soportado: " + column.typeName());
            };
        } catch (BackupValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BackupValidationException("Valor inválido para columna " + column.name(), ex);
        }
    }

    private BigInteger exactInteger(String value, long min, long max) {
        BigInteger result = new BigDecimal(value).toBigIntegerExact();
        if (result.compareTo(BigInteger.valueOf(min)) < 0 || result.compareTo(BigInteger.valueOf(max)) > 0) {
            throw new BackupValidationException("Valor entero fuera de rango");
        }
        return result;
    }

    private BigDecimal parseDecimal(String value, ColumnDefinition column) {
        BigDecimal decimal = new BigDecimal(value);
        if (decimal.precision() > column.size() || decimal.scale() > column.scale()) {
            throw new BackupValidationException("Valor decimal fuera de precisión/escala en " + column.name());
        }
        return decimal;
    }

    private Float finiteFloat(String value) {
        float parsed = Float.parseFloat(value);
        if (!Float.isFinite(parsed)) throw new BackupValidationException("Valor float no finito");
        return parsed;
    }

    private Double finiteDouble(String value) {
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) throw new BackupValidationException("Valor double no finito");
        return parsed;
    }

    private Boolean parseBoolean(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new BackupValidationException("Valor booleano inválido");
        }
        return Boolean.valueOf(value);
    }

    private List<TableDefinition> discoverTables(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String schema = connection.getSchema();
        Set<String> mappedNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
            Table mapping = entity.getJavaType().getAnnotation(Table.class);
            if (mapping == null || mapping.name().isBlank()) {
                throw new IllegalStateException("Entidad JPA sin nombre de tabla explícito: " + entity.getName());
            }
            if (Set.of("password_reset_tokens", "password_reset_quotas").contains(mapping.name())) continue;
            if (!mappedNames.add(mapping.name())) throw new IllegalStateException("Tabla JPA duplicada: " + mapping.name());
            // Element collections are persistent business data too. Omitting
            // refunds would lose history and can block parent-row restoration.
            for (var field : entity.getJavaType().getDeclaredFields()) {
                var collection = field.getAnnotation(jakarta.persistence.CollectionTable.class);
                if (field.isAnnotationPresent(jakarta.persistence.ElementCollection.class) && collection != null) {
                    if (collection.name().isBlank()) throw new IllegalStateException("Colección JPA sin tabla explícita");
                    mappedNames.add(collection.name());
                }
            }
        }
        Map<String, String> actualNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), schema, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableSchema = tables.getString("TABLE_SCHEM");
                if (tableSchema != null && tableSchema.equalsIgnoreCase(schema)) {
                    actualNames.put(tables.getString("TABLE_NAME"), tables.getString("TABLE_NAME"));
                }
            }
        }
        List<TableDefinition> definitions = new ArrayList<>();
        for (String mapped : mappedNames) {
            String actual = actualNames.get(mapped);
            if (actual == null) throw new IllegalStateException("No existe tabla JDBC para entidad mapeada " + mapped);
            List<ColumnDefinition> columns = readColumns(metadata, connection, schema, actual);
            List<String> primaryKey = readPrimaryKey(metadata, connection, schema, actual);
            List<List<String>> uniqueKeys = readUniqueKeys(metadata, connection, schema, actual, primaryKey);
            List<ForeignKey> foreignKeys = readForeignKeys(metadata, connection, schema, actual);
            definitions.add(new TableDefinition(actual, columns, primaryKey, uniqueKeys, foreignKeys));
        }
        definitions.sort((left, right) -> left.name().compareTo(right.name()));
        Set<String> allowlist = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        definitions.forEach(table -> allowlist.add(table.name()));
        for (TableDefinition table : definitions) {
            for (ForeignKey foreignKey : table.foreignKeys()) {
                if (!allowlist.contains(foreignKey.referencedTable())) {
                    throw new IllegalStateException("Clave foránea fuera del metamodelo JPA en " + table.name());
                }
            }
        }
        return definitions;
    }

    private List<ColumnDefinition> readColumns(DatabaseMetaData metadata, Connection connection, String schema, String table) throws SQLException {
        List<ColumnDefinition> columns = new ArrayList<>();
        try (ResultSet result = metadata.getColumns(connection.getCatalog(), schema, table, "%")) {
            while (result.next()) {
                int type = result.getInt("DATA_TYPE");
                String typeName = result.getString("TYPE_NAME");
                ensureSupportedType(type);
                columns.add(new ColumnDefinition(result.getString("COLUMN_NAME"), type, typeName,
                    result.getInt("COLUMN_SIZE"), result.getInt("DECIMAL_DIGITS"),
                    result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                    "YES".equalsIgnoreCase(result.getString("IS_AUTOINCREMENT")),
                    "YES".equalsIgnoreCase(result.getString("IS_GENERATEDCOLUMN")),
                    result.getInt("ORDINAL_POSITION")));
            }
        }
        columns.sort((left, right) -> Integer.compare(left.ordinal(), right.ordinal()));
        if (columns.isEmpty()) throw new IllegalStateException("Tabla sin columnas JDBC: " + table);
        if (columns.stream().anyMatch(column -> column.generated())) {
            throw new IllegalStateException("Columna calculada no soportada en tabla JPA " + table);
        }
        return columns;
    }

    private List<String> readPrimaryKey(DatabaseMetaData metadata, Connection connection, String schema, String table) throws SQLException {
        Map<Short, String> ordered = new TreeMap<>();
        try (ResultSet result = metadata.getPrimaryKeys(connection.getCatalog(), schema, table)) {
            while (result.next()) ordered.put(result.getShort("KEY_SEQ"), result.getString("COLUMN_NAME"));
        }
        return List.copyOf(ordered.values());
    }

    private List<List<String>> readUniqueKeys(DatabaseMetaData metadata, Connection connection, String schema, String table,
            List<String> primaryKey) throws SQLException {
        Map<String, Map<Short, String>> indexes = new TreeMap<>();
        try (ResultSet result = metadata.getIndexInfo(connection.getCatalog(), schema, table, true, false)) {
            while (result.next()) {
                String index = result.getString("INDEX_NAME");
                String column = result.getString("COLUMN_NAME");
                if (index != null && column != null) indexes.computeIfAbsent(index, ignored -> new TreeMap<>())
                    .put(result.getShort("ORDINAL_POSITION"), column);
            }
        }
        Set<List<String>> unique = new TreeSet<>((left, right) -> left.toString().compareTo(right.toString()));
        indexes.values().stream().map(columns -> List.copyOf(columns.values()))
            .filter(columns -> !columns.isEmpty() && !columns.equals(primaryKey)).forEach(unique::add);
        return List.copyOf(unique);
    }

    private List<ForeignKey> readForeignKeys(DatabaseMetaData metadata, Connection connection, String schema, String table) throws SQLException {
        Map<String, Map<Short, ForeignKeyPart>> groups = new TreeMap<>();
        try (ResultSet result = metadata.getImportedKeys(connection.getCatalog(), schema, table)) {
            while (result.next()) {
                String name = result.getString("FK_NAME");
                if (name == null) name = result.getString("FKTABLE_NAME") + "_" + result.getString("KEY_SEQ");
                groups.computeIfAbsent(name, ignored -> new TreeMap<>()).put(result.getShort("KEY_SEQ"), new ForeignKeyPart(
                    result.getString("FKCOLUMN_NAME"), result.getString("PKTABLE_NAME"), result.getString("PKCOLUMN_NAME")));
            }
        }
        List<ForeignKey> result = new ArrayList<>();
        for (Map<Short, ForeignKeyPart> group : groups.values()) {
            ForeignKeyPart first = group.values().iterator().next();
            List<String> columns = group.values().stream().map(part -> part.column()).toList();
            List<String> referencedColumns = group.values().stream().map(part -> part.referencedColumn()).toList();
            result.add(new ForeignKey(columns, first.referencedTable(), referencedColumns));
        }
        result.sort((left, right) -> left.toString().compareTo(right.toString()));
        return List.copyOf(result);
    }

    private List<TableDefinition> dependencyOrder(List<TableDefinition> tables) {
        Map<String, TableDefinition> byName = new TreeMap<>();
        Map<String, Set<String>> dependencies = new TreeMap<>();
        tables.forEach(table -> {
            byName.put(table.name(), table);
            dependencies.put(table.name(), new TreeSet<>());
        });
        for (TableDefinition table : tables) {
            for (ForeignKey foreignKey : table.foreignKeys()) {
                if (!foreignKey.referencedTable().equalsIgnoreCase(table.name())) {
                    dependencies.get(table.name()).add(foreignKey.referencedTable());
                } else {
                    throw new BackupValidationException("Ciclo de clave foránea no resoluble en " + table.name());
                }
            }
        }
        List<TableDefinition> ordered = new ArrayList<>();
        while (!dependencies.isEmpty()) {
            List<String> ready = dependencies.entrySet().stream().filter(entry -> entry.getValue().isEmpty())
                .map(entry -> entry.getKey()).toList();
            if (ready.isEmpty()) throw new BackupValidationException("El esquema contiene un ciclo de claves foráneas no resoluble");
            for (String name : ready) {
                ordered.add(byName.get(name));
                dependencies.remove(name);
            }
            dependencies.values().forEach(remaining -> remaining.removeAll(ready));
        }
        return ordered;
    }

    private String fingerprint(List<TableDefinition> definitions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (TableDefinition table : definitions) {
                update(digest, "TABLE");
                update(digest, table.name());
                update(digest, Integer.toString(table.columns().size()));
                for (ColumnDefinition column : table.columns()) {
                    update(digest, column.name());
                    update(digest, Integer.toString(column.jdbcType()));
                    update(digest, column.typeName());
                    update(digest, Integer.toString(column.size()));
                    update(digest, Integer.toString(column.scale()));
                    update(digest, Boolean.toString(column.nullable()));
                    update(digest, Boolean.toString(column.autoIncrement()));
                    update(digest, Boolean.toString(column.generated()));
                }
                update(digest, "PK");
                update(digest, Integer.toString(table.primaryKey().size()));
                table.primaryKey().forEach(value -> update(digest, value));
                update(digest, "UKS");
                update(digest, Integer.toString(table.uniqueKeys().size()));
                table.uniqueKeys().forEach(key -> {
                    update(digest, Integer.toString(key.size()));
                    key.forEach(value -> update(digest, value));
                });
                update(digest, "FKS");
                update(digest, Integer.toString(table.foreignKeys().size()));
                table.foreignKeys().forEach(key -> {
                    update(digest, Integer.toString(key.columns().size()));
                    key.columns().forEach(value -> update(digest, value));
                    update(digest, key.referencedTable());
                    key.referencedColumns().forEach(value -> update(digest, value));
                });
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private ObjectNode writeDefinition(TableDefinition definition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", definition.name());
        node.set("columns", writeColumns(definition.columns()));
        node.set("primaryKey", writeStrings(definition.primaryKey()));
        node.set("uniqueKeys", writeStringGroups(definition.uniqueKeys()));
        node.set("foreignKeys", writeForeignKeys(definition.foreignKeys()));
        return node;
    }

    private ArrayNode writeColumns(List<ColumnDefinition> columns) {
        ArrayNode result = objectMapper.createArrayNode();
        for (ColumnDefinition column : columns) {
            ObjectNode node = result.addObject();
            node.put("name", column.name());
            node.put("jdbcType", column.jdbcType());
            node.put("typeName", column.typeName());
            node.put("size", column.size());
            node.put("scale", column.scale());
            node.put("nullable", column.nullable());
            node.put("autoIncrement", column.autoIncrement());
            node.put("generated", column.generated());
        }
        return result;
    }

    private ArrayNode writeStrings(List<String> values) {
        ArrayNode result = objectMapper.createArrayNode();
        values.forEach(result::add);
        return result;
    }

    private ArrayNode writeStringGroups(List<List<String>> groups) {
        ArrayNode result = objectMapper.createArrayNode();
        for (List<String> group : groups) result.add(writeStrings(group));
        return result;
    }

    private ArrayNode writeForeignKeys(List<ForeignKey> foreignKeys) {
        ArrayNode result = objectMapper.createArrayNode();
        for (ForeignKey key : foreignKeys) {
            ObjectNode node = result.addObject();
            node.set("columns", writeStrings(key.columns()));
            node.put("referencedTable", key.referencedTable());
            node.set("referencedColumns", writeStrings(key.referencedColumns()));
        }
        return result;
    }

    private ArrayNode requiredArray(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) throw new BackupValidationException("Campo " + field + " inválido");
        return (ArrayNode) value;
    }

    private void requireFields(JsonNode node, Set<String> expected, String description) {
        if (node == null || !node.isObject()) throw new BackupValidationException("Objeto " + description + " inválido");
        Set<String> actual = new HashSet<>();
        node.propertyNames().forEach(actual::add);
        if (!actual.equals(expected)) throw new BackupValidationException("Campos desconocidos o faltantes en " + description);
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) throw new BackupValidationException("Campo " + field + " inválido");
        return value.textValue();
    }

    private int columnIndex(TableDefinition definition, String column) {
        for (int i = 0; i < definition.columns().size(); i++) {
            if (definition.columns().get(i).name().equals(column)) return i;
        }
        throw new BackupValidationException("Columna de relación desconocida");
    }

    private boolean isBinary(int type) {
        return type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY;
    }

    private boolean isCharacter(int type) {
        return switch (type) {
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR,
                    Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> true;
            default -> false;
        };
    }

    private boolean isTemporal(int type) {
        return type == Types.DATE || type == Types.TIME || type == Types.TIMESTAMP
            || type == Types.TIME_WITH_TIMEZONE || type == Types.TIMESTAMP_WITH_TIMEZONE;
    }

    private void ensureSupportedType(int type) {
        if (isBinary(type) || isCharacter(type) || isTemporal(type)) return;
        switch (type) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.NUMERIC,
                    Types.DECIMAL, Types.FLOAT, Types.REAL, Types.DOUBLE, Types.BOOLEAN, Types.BIT -> { }
            default -> throw new IllegalStateException("Tipo JDBC no soportado en entidad JPA: " + type);
        }
    }

    private String quote(Connection connection, String identifier) {
        try {
            String quote = connection.getMetaData().getIdentifierQuoteString().trim();
            if (quote.isEmpty()) throw new IllegalStateException("El driver JDBC no ofrece quote de identificadores");
            return quote + identifier.replace(quote, quote + quote) + quote;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo obtener el quote JDBC", ex);
        }
    }

    private void requireH2(DatabaseMetaData metadata) throws SQLException {
        if (!metadata.getDatabaseProductName().toUpperCase(java.util.Locale.ROOT).contains("H2")) {
            throw new IllegalStateException("El respaldo lógico requiere una base H2");
        }
    }

    public record RestoreSummary(int tables, long rows) { }
    private record ColumnDefinition(String name, int jdbcType, String typeName, int size, int scale,
            boolean nullable, boolean autoIncrement, boolean generated, int ordinal) { }
    private record ForeignKey(List<String> columns, String referencedTable, List<String> referencedColumns) { }
    private record ForeignKeyPart(String column, String referencedTable, String referencedColumn) { }
    private record TableDefinition(String name, List<ColumnDefinition> columns, List<String> primaryKey,
            List<List<String>> uniqueKeys, List<ForeignKey> foreignKeys) { }
    private record ParsedRow(List<Object> values) { }
    private record ParsedTable(TableDefinition definition, List<ParsedRow> rows) { }
    private record ParsedBackup(Map<String, ParsedTable> tables, long rowCount) { }
    private record IdentityRestart(String table, String column, BigInteger nextValue) { }
}
