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

import com.github.jinahya.database.metadata.bind.Table;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * An abstract test class for in-memory databases.
 *
 * @author Jin Kwon &lt;onacit at gmail.com&gt;
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
abstract class _Metadata_Binding_Test {

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

    @Disabled
    @Test
    void getColumnPrivileges__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getColumnPrivileges(null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
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
    @Disabled
    @Test
    void getBestRowIdentifier__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getBestRowIdentifier(null, null, null, DatabaseMetaData.bestRowTemporary, true);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getBestRowIdentifier(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Disabled
    @Test
    void getVersionColumns__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getVersionColumns(null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getVersionColumns(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Disabled
    @Test
    void getPrimaryKeys__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getPrimaryKeys(null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getPrimaryKeys(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

//    @Test
//    void getImportedKeys__() throws Throwable {
//        bind(md -> md.getImportedKeys(null, null, null));
//    }

//    @Test
//    void getExportedKeys__() throws Throwable {
//        bind(md -> md.getExportedKeys(null, null, null));
//    }

    @Disabled
    @Test
    void getCrossReference__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getCrossReference(null, null, null, null, null, null);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getCrossReference(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    @Disabled
    @Test
    void getIndexInfo__() throws Throwable {
        applyMetadata(m -> {
            try {
                final var results = m.getIndexInfo(null, null, null, false, true);
                __ResultSetMetaDataColumnTestUtils.log(results);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("getIndexInfo(), unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
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

    @Disabled
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
    void tables(final Collection<? extends Table> tables) throws Throwable {
        for (final var table : tables) {
            acceptMetadata(m -> {
                {
                    final var result = m.getTablePrivileges(
                            Optional.ofNullable(table.getTableCat()).orElse(""),
                            Optional.ofNullable(table.getTableSchem()).orElse(""),
                            table.getTableName()
                    );
                    __ResultSetMetaDataColumnTestUtils.log(result);
                }
            });
        }
        log.debug("tables({})", tables);
    }
}
