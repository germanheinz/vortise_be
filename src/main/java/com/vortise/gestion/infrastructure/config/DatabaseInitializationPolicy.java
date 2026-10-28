package com.vortise.gestion.infrastructure.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!staging & !prod & !production")
public class DatabaseInitializationPolicy implements SeedRunPolicy, SmartInitializingSingleton {
    private final DataSource dataSource;
    private volatile boolean freshDatabase;

    public DatabaseInitializationPolicy(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.freshDatabase = isDatabaseEmpty();
    }

    @Override
    public boolean shouldRunSeeders() {
        return freshDatabase;
    }

    private boolean isDatabaseEmpty() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String schema = connection.getSchema();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), schema, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableSchema = tables.getString("TABLE_SCHEM");
                    if (tableSchema == null || !tableSchema.equalsIgnoreCase(schema)) continue;
                    String tableName = tables.getString("TABLE_NAME");
                    String quote = metadata.getIdentifierQuoteString().trim();
                    String identifier = quote + tableName.replace(quote, quote + quote) + quote;
                    try (var statement = connection.createStatement();
                            ResultSet row = statement.executeQuery("SELECT 1 FROM " + identifier + " FETCH FIRST 1 ROW ONLY")) {
                        if (row.next()) return false;
                    }
                }
            }
            return true;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo determinar si la base está vacía", ex);
        }
    }
}
