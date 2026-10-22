package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DatabaseInitializationPolicyTest {
    @Test
    void freezesPopulatedDatabaseDecisionAtStartup() throws Exception {
        Fixture fixture = fixture(true, true);
        DatabaseInitializationPolicy policy = new DatabaseInitializationPolicy(fixture.dataSource());

        policy.afterSingletonsInstantiated();
        assertFalse(policy.shouldRunSeeders());

        when(fixture.rows().next()).thenReturn(false);
        assertFalse(policy.shouldRunSeeders());
        verify(fixture.dataSource()).getConnection();
    }

    @Test
    void freezesFreshDatabaseDecisionAtStartup() throws Exception {
        Fixture fixture = fixture(false, false);
        DatabaseInitializationPolicy policy = new DatabaseInitializationPolicy(fixture.dataSource());

        policy.afterSingletonsInstantiated();
        assertTrue(policy.shouldRunSeeders());

        when(fixture.tables().next()).thenReturn(true);
        assertTrue(policy.shouldRunSeeders());
        verify(fixture.dataSource()).getConnection();
    }

    private Fixture fixture(boolean hasTable, boolean hasRows) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        ResultSet rows = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.getSchema()).thenReturn("PUBLIC");
        when(metadata.getTables(any(), any(), any(), any())).thenReturn(tables);
        when(metadata.getIdentifierQuoteString()).thenReturn("\"");
        when(tables.next()).thenReturn(hasTable, false);
        when(tables.getString("TABLE_SCHEM")).thenReturn("PUBLIC");
        when(tables.getString("TABLE_NAME")).thenReturn("COMPANIES");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1 FROM \"COMPANIES\" FETCH FIRST 1 ROW ONLY")).thenReturn(rows);
        when(rows.next()).thenReturn(hasRows);
        return new Fixture(dataSource, tables, rows);
    }

    private record Fixture(DataSource dataSource, ResultSet tables, ResultSet rows) { }
}