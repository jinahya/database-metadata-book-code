package io.github.jinahya.database.metadata.book;

/*-
 * #%L
 * database-metadata-book-code
 * %%
 * Copyright (C) 2011 - 2019 Jinahya, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * An abstract test class for in-memory databases.
 *
 * @author Jin Kwon &lt;onacit at gmail.com&gt;
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
abstract class _Metadata_Raw_Test {

    /**
     * Returns a connection
     *
     * @return a connection.
     * @throws SQLException if a database error occurs.
     */
    abstract Connection connect() throws SQLException;

    // -----------------------------------------------------------------------------------------------------------------
    <R> R applyConnection(final CheckedFunction1<? super Connection, ? extends R> function) throws Throwable {
        return __JavaSqlTestUtils.applyConnection(this::connect, function);
    }

    <R> R applyMetadata(final CheckedFunction1<? super DatabaseMetaData, ? extends R> function) throws Throwable {
        try {
            return __JavaSqlTestUtils.applyDatabaseMetaData(this::connect, function);
        } catch (final SQLFeatureNotSupportedException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    void acceptMetadata(final CheckedConsumer<? super DatabaseMetaData> consumer) throws Throwable {
        applyMetadata(m -> {
            consumer.accept(m);
            return null;
        });
    }

//    <R> R applyContext(final CheckedFunction1<? super Context, ? extends R> function) throws Throwable {
//        return applyConnection(c -> function.apply(Context.from(c)));
//    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Applies the specified function to a newly opened {@link DatabaseMetaData} and logs column metadata for the result
     * set it returns. Methods unsupported by the driver, or failing with an {@link SQLException}, are logged rather
     * than failing the test.
     *
     * @param function a function returning the result set to log.
     * @throws Throwable if opening a connection fails.
     */
    private void bind(final CheckedFunction1<? super DatabaseMetaData, ? extends ResultSet> function) throws Throwable {
        requireNonNull(function, "function is null");
        applyMetadata(md -> {
            try (var results = function.apply(md)) {
                if (results != null) {
                    __ResultSetMetaDataColumnTestUtils.log(results);
                }
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("unsupported by the driver; {}", sqlfnse.getMessage());
            } catch (final SQLException sqle) {
                log.error("failed to apply function", sqle);
            }
            return null;
        });
    }

    // ----------------------------------------------------------------------------------------------------- fixture
    /**
     * A minimal, standard-SQL fixture: a parent with a two-column primary key {@code (B_COL, A_COL)}, a child with a
     * composite foreign key back to it, and an index on the child's foreign-key columns. The methods that require an
     * existing table (most drivers reject a {@code null} table) query this fixture.
     */
    private static final java.util.List<String> FIXTURE_DDL = java.util.List.of(
            """
            create table demo_parent (
                a_col integer not null,
                b_col integer not null,
                constraint pk_demo_pk primary key (b_col, a_col)
            )""",
            """
            create table demo_child (
                id integer not null primary key,
                parent_a integer,
                parent_b integer,
                constraint fk_demo_child foreign key (parent_b, parent_a)
                        references demo_parent (b_col, a_col)
            )""",
            "create index ix_demo_child on demo_child (parent_b, parent_a)");

    /**
     * The coordinates of a table as the driver actually stores them; identifier case-folding differs per engine, so we
     * discover the stored name rather than assume it.
     *
     * @param cat   the {@code TABLE_CAT}.
     * @param schem the {@code TABLE_SCHEM}.
     * @param name  the {@code TABLE_NAME}.
     */
    record TableRef(String cat, String schem, String name) {

    }

    private void createFixture(final DatabaseMetaData md) throws SQLException {
        try (var statement = md.getConnection().createStatement()) {
            for (final var ddl : FIXTURE_DDL) {
                try {
                    statement.execute(ddl);
                } catch (final SQLException sqle) {
                    log.debug("fixture DDL skipped ({}): {}", sqle.getMessage(), ddl.lines().findFirst().orElse(""));
                }
            }
        }
    }

    private TableRef find(final DatabaseMetaData md, final String name) throws SQLException {
        try (var tables = md.getTables(null, null, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                final var stored = tables.getString("TABLE_NAME");
                if (stored != null && stored.equalsIgnoreCase(name)) {
                    return new TableRef(tables.getString("TABLE_CAT"), tables.getString("TABLE_SCHEM"), stored);
                }
            }
        }
        return null;
    }

    /**
     * Creates the fixture and returns the stored coordinates of the given table, or {@code null} if the driver did not
     * create/expose it.
     */
    private TableRef fixtureTable(final DatabaseMetaData md, final String name) throws SQLException {
        createFixture(md);
        return find(md, name);
    }

    // ------------------------------------------------------------------------------------------- (scalar) terms/values
    @Test
    void getSQLKeywords__() throws Throwable {
        acceptMetadata(m -> {
            final var result = m.getSQLKeywords();
            log.debug("sql keywords: {}", result);
        });
    }

    @Test
    void getCatalogTerm__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var result = m.getCatalogTerm();
                log.info("catalogTerm: {}", result);
            } catch (final SQLException sqle) {
                log.error("failed to get catalogTerm", sqle);
            }
            return null;
        });
    }

    @Test
    void getSchemaTerm__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var result = m.getSchemaTerm();
                log.info("schemaTerm: {}", result);
            } catch (final SQLException sqle) {
                log.error("failed to get schemaTerm", sqle);
            }
            return null;
        });
    }

    @Test
    void getProcedureTerm__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var result = m.getProcedureTerm();
                log.info("procedureTerm: {}", result);
            } catch (final SQLException sqle) {
                log.error("failed to get procedureTerm", sqle);
            }
            return null;
        });
    }

    // ----------------------------------------------------------------------------------------------------- getCatalogs
    @Test
    void getCatalogs__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getCatalogs();
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getCatalogs(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    // ------------------------------------------------------------------------------------------------------ getSchemas
    @Test
    void getSchemas__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getSchemas();
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getSchemas(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getSchemas_catalog_schemaPattern() throws Throwable {
        applyMetadata(m -> {
            final var catalogs = m.getCatalogs();
            while (catalogs.next()) {
                final var tableCat = catalogs.getString("TABLE_CAT");
                log.debug("tableCat: {}", tableCat);
                final var effectiveTableCat = Optional.ofNullable(tableCat).map(String::strip).orElse("");
                log.debug("effectiveTableCat: {}", effectiveTableCat);
                try {
                    final var schemas = m.getSchemas(effectiveTableCat, "%");
                    __ResultSetMetaDataColumnTestUtils.log(schemas);
                } catch (final SQLFeatureNotSupportedException sqlfnse) {
                    log.debug("getSchemas(...), unsupported by the driver; {}", sqlfnse.getMessage());
                }
            }
            return null;
        });
    }

    // --------------------------------------------------------------------------------------------------- getTableTypes
    @Test
    void getTableTypes__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getTableTypes();
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getTableTypes(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    // ------------------------------------------------------------------------------------------------------- getTables
    @Test
    void getTables__() throws Throwable {
        bind(m -> {
            final var tables = m.getTables(null, null, null, null);
            return tables;
        });
    }

    // ------------------------------------------------------------------------------------------------------ getColumns
    @Test
    void getColumns__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getColumns(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getColumns(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getColumnPrivileges__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_parent");
            if (t == null) {
                return;
            }
            try (var results = m.getColumnPrivileges(t.cat(), t.schem(), t.name(), "%")) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getColumnPrivileges(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getTablePrivileges__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getTablePrivileges(null, null, "%");
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getTablePrivileges(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getPseudoColumns__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getPseudoColumns(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.error("unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    // --------------------------------------------------------------------------------------------- row identifier/keys
    @Test
    void getBestRowIdentifier__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_parent");
            if (t == null) {
                return;
            }
            try (var results = m.getBestRowIdentifier(
                    t.cat(), t.schem(), t.name(), DatabaseMetaData.bestRowTemporary, true)) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getBestRowIdentifier(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getVersionColumns__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_parent");
            if (t == null) {
                return;
            }
            try (var results = m.getVersionColumns(t.cat(), t.schem(), t.name())) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getVersionColumns(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getPrimaryKeys__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_parent");
            if (t == null) {
                return;
            }
            try (var results = m.getPrimaryKeys(t.cat(), t.schem(), t.name())) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getPrimaryKeys(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getImportedKeys__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_child"); // the child imports the parent's key
            if (t == null) {
                return;
            }
            try (var results = m.getImportedKeys(t.cat(), t.schem(), t.name())) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getImportedKeys(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getExportedKeys__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_parent"); // the parent exports its key to the child
            if (t == null) {
                return;
            }
            try (var results = m.getExportedKeys(t.cat(), t.schem(), t.name())) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getExportedKeys(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getCrossReference__() throws Throwable {
        acceptMetadata(m -> {
            final var p = fixtureTable(m, "demo_parent");
            final var c = find(m, "demo_child"); // fixture already created by the call above
            if (p == null || c == null) {
                return;
            }
            try (var results = m.getCrossReference(
                    p.cat(), p.schem(), p.name(), c.cat(), c.schem(), c.name())) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getCrossReference(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    @Test
    void getIndexInfo__() throws Throwable {
        acceptMetadata(m -> {
            final var t = fixtureTable(m, "demo_child"); // the child carries the index
            if (t == null) {
                return;
            }
            try (var results = m.getIndexInfo(t.cat(), t.schem(), t.name(), false, true)) {
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getIndexInfo(...), unsupported by the driver; {}", sqlfnse.getMessage());
            }
        });
    }

    // ----------------------------------------------------------------------------------------------------- getTypeInfo
    @Test
    void getTypeInfo__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getTypeInfo();
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getTypeInfo(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    // ------------------------------------------------------------------------------------------------------ UDTs/types
    @Test
    void getUDTs__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getUDTs(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getUDTs(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getSuperTypes__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getSuperTypes(null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getSuperTypes(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getSuperTables__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getSuperTables(null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getSuperTables(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getAttributes__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getAttributes(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getAttributes(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    // -------------------------------------------------------------------------------------------- procedures/functions
    @Test
    void getProcedures__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getProcedures(null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getProcedures(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getProcedureColumns__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getProcedureColumns(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getProcedureColumns(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getFunctions__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getFunctions(null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getFunctions(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Test
    void getFunctionColumns__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getFunctionColumns(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getFunctionColumns(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    // ----------------------------------------------------------------------------------------- getClientInfoProperties
    @Test
    void getClientInfoProperties__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getClientInfoProperties();
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.error(sqlfnse.getMessage(), sqlfnse);
            }
            return null;
        });
    }

    // -----------------------------------------------------------------------------------------------------------------
    void tables() throws Throwable {
        acceptMetadata(m -> {
            // collect table coordinates first, so no getTables() cursor stays open while querying privileges
            final var coordinates = new ArrayList<String[]>();
            try (var tables = m.getTables(null, null, null, null)) {
                while (tables.next()) {
                    coordinates.add(new String[] {
                            Optional.ofNullable(tables.getString("TABLE_CAT")).orElse(""),
                            Optional.ofNullable(tables.getString("TABLE_SCHEM")).orElse(""),
                            tables.getString("TABLE_NAME")
                    });
                }
            }
            for (final var coordinate : coordinates) {
                try (var results = m.getTablePrivileges(coordinate[0], coordinate[1], coordinate[2])) {
                    __ResultSetMetaDataColumnTestUtils.log(results);
                }
            }
            log.debug("tables({})", coordinates.size());
        });
    }
}
