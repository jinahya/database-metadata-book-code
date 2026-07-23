package io.github.jinahya.database.metadata.book;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A binding test against HSQLDB.
 *
 * @author Jin Kwon &lt;jinahya_at_gmail.com&gt;
 */
@Slf4j
class Binding_Hsql_Test
        extends _Metadata_Binding_Test {

    private static final String DRIVER_NAME = "org.hsqldb.jdbc.JDBCDriver";

    private static final Class<?> DRIVER_CLASS;

    static {
        try {
            DRIVER_CLASS = Class.forName(DRIVER_NAME);
        } catch (final ClassNotFoundException cnfe) {
            throw new InstantiationError(cnfe.getMessage());
        }
    }

    private static final String CONNECTION_URL = "jdbc:hsqldb:mem:test";

    @Override
    Connection connect() throws SQLException {
        return __JavaSqlTestUtils.connection(CONNECTION_URL);
    }
}
