package io.github.jinahya.database.metadata.book;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A binding test against SQLite.
 *
 * @author Jin Kwon &lt;jinahya_at_gmail.com&gt;
 */
@Slf4j
class Binding_Sqlite_Test
        extends _Metadata_Binding_Test {

    private static final String CONNECTION_URL = "jdbc:sqlite::memory:";

    @Override
    Connection connect() throws SQLException {
        return __JavaSqlTestUtils.connection(CONNECTION_URL);
    }
}
