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

import io.vavr.CheckedFunction1;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static java.util.Objects.requireNonNull;

/**
 * An abstract test class for in-memory databases.
 *
 * @author Jin Kwon &lt;onacit at gmail.com&gt;
 */
@Slf4j
abstract class Memory_$_Test
        extends _Metadata_Test {

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
        return __JavaSqlTestUtils.applyDatabaseMetaData(this::connect, function);
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
}
