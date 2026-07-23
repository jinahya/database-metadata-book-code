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

import com.github.jinahya.database.metadata.bind.Context;
import com.github.jinahya.database.metadata.bind.PrimaryKey;
import com.github.jinahya.database.metadata.bind.Table;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Comparator;
import java.util.List;

/**
 * An abstract test class exercising the {@code database-metadata-bind} library ({@link Context}) rather than the raw
 * {@link java.sql.DatabaseMetaData} API. Each concrete subclass targets one in-memory engine.
 *
 * @author Jin Kwon &lt;onacit at gmail.com&gt;
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
abstract class _Metadata_Binding_Test {

    /**
     * Returns a connection.
     *
     * @return a connection.
     * @throws SQLException if a database error occurs.
     */
    abstract Connection connect() throws SQLException;

    // ----------------------------------------------------------------------------------------------------- fixture
    /**
     * The same standard-SQL fixture used by {@link _Metadata_Raw_Test}: a parent with a two-column primary key
     * {@code (B_COL, A_COL)}, a child with a composite foreign key back to it, and an index on the child's foreign-key
     * columns.
     */
    private static final List<String> FIXTURE_DDL = List.of(
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

    private void createFixture(final Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            for (final var ddl : FIXTURE_DDL) {
                try {
                    statement.execute(ddl);
                } catch (final SQLException sqle) {
                    log.debug("fixture DDL skipped ({}): {}", sqle.getMessage(), ddl.lines().findFirst().orElse(""));
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Opens a connection, creates the fixture, wraps the connection in a {@link Context}, and applies the specified
     * function.
     *
     * @param function the function to apply to the context.
     * @param <R>      the result type.
     * @return the result of the function.
     * @throws Throwable if an error occurs.
     */
    <R> R applyContext(final CheckedFunction1<? super Context, ? extends R> function) throws Throwable {
        try (var connection = connect()) {
            createFixture(connection);
            return function.apply(Context.newInstance(connection));
        }
    }

    /**
     * Applies the specified consumer to a {@link Context}, logging (rather than failing on) methods the driver does not
     * support.
     *
     * @param consumer the consumer to accept the context.
     * @throws Throwable if an error occurs.
     */
    void acceptContext(final CheckedConsumer<? super Context> consumer) throws Throwable {
        applyContext(context -> {
            try {
                consumer.accept(context);
            } catch (final SQLFeatureNotSupportedException sqlfnse) {
                log.debug("unsupported by the driver; {}", sqlfnse.getMessage());
            }
            return null;
        });
    }

    /**
     * Finds the fixture table by name, honoring per-engine identifier case-folding.
     *
     * @param context the context.
     * @param name    the (unquoted) table name to match, ignoring case.
     * @return the matching {@link Table}, or {@code null}.
     * @throws SQLException if a database error occurs.
     */
    private Table findTable(final Context context, final String name) throws SQLException {
        return context.getTables(null, null, "%", new String[] {"TABLE"}).stream()
                .filter(t -> name.equalsIgnoreCase(t.getTableName()))
                .findFirst()
                .orElse(null);
    }

    // --------------------------------------------------------------------------------------------------- namespace
    @Test
    void getCatalogs__() throws Throwable {
        acceptContext(context -> {
            final var catalogs = context.getCatalogs();
            catalogs.forEach(c -> log.info("catalog: {}", c.getTableCat()));
        });
    }

    @Test
    void getSchemas__() throws Throwable {
        acceptContext(context -> {
            final var schemas = context.getSchemas(null, null);
            schemas.forEach(s -> log.info("schema: {}.{}", s.getTableCatalog(), s.getTableSchem()));
        });
    }

    // ------------------------------------------------------------------------------------------------------- tables
    @Test
    void getTables__() throws Throwable {
        acceptContext(context -> {
            final var tables = context.getTables(null, null, "%", null);
            tables.forEach(t -> log.info("table: {} ({})", t.getTableName(), t.getTableType()));
        });
    }

    @Test
    void getColumns__() throws Throwable {
        acceptContext(context -> {
            final var parent = findTable(context, "demo_parent");
            if (parent == null) {
                return;
            }
            final var columns = context.getColumns(
                    parent.getTableCat(), parent.getTableSchem(), parent.getTableName(), "%");
            columns.forEach(c -> log.info(
                    "column: {} {} nullable={} ordinal={}",
                    c.getColumnName(), c.getTypeName(), c.getNullable(), c.getOrdinalPosition()));
        });
    }

    // --------------------------------------------------------------------------------------------------------- keys
    @Test
    void getPrimaryKeys__() throws Throwable {
        acceptContext(context -> {
            final var parent = findTable(context, "demo_parent");
            if (parent == null) {
                return;
            }
            final List<PrimaryKey> keys = context.getPrimaryKeys(
                    parent.getTableCat(), parent.getTableSchem(), parent.getTableName());
            // documented order is COLUMN_NAME; the wrapper Integer getKeySeq() lets us re-sort to the real key order,
            // preserving a driver-returned null instead of collapsing it to 0.
            keys.sort(Comparator.comparing(
                    PrimaryKey::getKeySeq, Comparator.nullsFirst(Comparator.naturalOrder())));
            keys.forEach(k -> log.info(
                    "pk: {} seq={} name={}", k.getColumnName(), k.getKeySeq(), k.getPkName()));
        });
    }

    @Test
    void getImportedKeys__() throws Throwable {
        acceptContext(context -> {
            final var child = findTable(context, "demo_child");
            if (child == null) {
                return;
            }
            final var keys = context.getImportedKeys(
                    child.getTableCat(), child.getTableSchem(), child.getTableName());
            keys.forEach(k -> log.info(
                    "fk: {}.{} -> {}.{} seq={}",
                    k.getFktableName(), k.getFkcolumnName(),
                    k.getPktableName(), k.getPkcolumnName(), k.getKeySeq()));
        });
    }

    @Test
    void getIndexInfo__() throws Throwable {
        acceptContext(context -> {
            final var child = findTable(context, "demo_child");
            if (child == null) {
                return;
            }
            final var indexes = context.getIndexInfo(
                    child.getTableCat(), child.getTableSchem(), child.getTableName(), false, true);
            indexes.forEach(i -> log.info(
                    "index: {} nonUnique={} column={}",
                    i.getIndexName(), i.getNonUnique(), i.getColumnName()));
        });
    }

    // -------------------------------------------------------------------------------------------------------- types
    @Test
    void getTypeInfo__() throws Throwable {
        acceptContext(context -> {
            final var types = context.getTypeInfo();
            types.forEach(t -> log.info("type: {} ({})", t.getTypeName(), t.getDataType()));
        });
    }

    // ------------------------------------------------------------------------------------------ procedures/functions
    @Test
    void getFunctions__() throws Throwable {
        acceptContext(context -> {
            final var functions = context.getFunctions(null, null, "%");
            functions.forEach(f -> log.info("function: {}", f.getFunctionName()));
        });
    }

    // ----------------------------------------------------------------------------------------- getClientInfoProperties
    @Test
    void getClientInfoProperties__() throws Throwable {
        acceptContext(context -> {
            final var properties = context.getClientInfoProperties();
            properties.forEach(p -> log.info("clientInfo: {} (max={})", p.getName(), p.getMaxLen()));
        });
    }
}
