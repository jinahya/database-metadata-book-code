package io.github.jinahya.database.metadata.book;

import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

@Slf4j
final class __JavaSqlResultSetMetaDataUtils {

    static void log(final ResultSetMetaData metaData) throws SQLException {
        Objects.requireNonNull(metaData, "metaData is null");
        final var columnCount = metaData.getColumnCount();
        for (var column = 1; column <= columnCount; column++) {
            log.info(
                    "column[{}]: label={}, name={}, type={} ({}), className={}, displaySize={}, precision={}, "
                    + "scale={}, catalog={}, schema={}, table={}, nullable={}, autoIncrement={}, caseSensitive={}, "
                    + "currency={}, definitelyWritable={}, readOnly={}, searchable={}, signed={}, writable={}",
                    column,
                    metaData.getColumnLabel(column),
                    metaData.getColumnName(column),
                    metaData.getColumnType(column),
                    metaData.getColumnTypeName(column),
                    metaData.getColumnClassName(column),
                    metaData.getColumnDisplaySize(column),
                    metaData.getPrecision(column),
                    metaData.getScale(column),
                    metaData.getCatalogName(column),
                    metaData.getSchemaName(column),
                    metaData.getTableName(column),
                    metaData.isNullable(column),
                    metaData.isAutoIncrement(column),
                    metaData.isCaseSensitive(column),
                    metaData.isCurrency(column),
                    metaData.isDefinitelyWritable(column),
                    metaData.isReadOnly(column),
                    metaData.isSearchable(column),
                    metaData.isSigned(column),
                    metaData.isWritable(column)
            );
        }
    }

    private __JavaSqlResultSetMetaDataUtils() {
        throw new AssertionError("instantiation is not allowed");
    }
}
