package io.github.jinahya.database.metadata.book;

import com.github.jinahya.sql.resultset.metadata.bind.ResultSetMetaDataColumn;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
final class __ResultSetMetaDataColumnTestUtils {

    static int maxColumnLabelLen(final List<ResultSetMetaDataColumn> columns) {
        return columns.stream().mapToInt(c -> c.getColumnLabel().length()).max().orElse(0);
    }

    static void log(final ResultSet results) throws SQLException {
        final var columns = ResultSetMetaDataColumn.bind(results);
        final var width = maxColumnLabelLen(columns);
        final var format = "%%-%ds".formatted(width); // e.g. "%-12s"; left-align each label within <width>
        for (int i = 0; results.next(); i++) {
            for (final var column : columns) {
                final var label = column.getColumnLabel();
                final var value = results.getObject(label);
                log.info("[{}] {}: {}", String.format("%02d", i), String.format(format, label), value);
            }
        }
    }

    static void process(final ResultSet results) throws SQLException {
        final var columns = ResultSetMetaDataColumn.bind(results);
        final var width = maxColumnLabelLen(columns);
        final var format = "%%-%ds".formatted(width); // e.g. "%-12s"; left-align each label within <width>
        for (int i = 0; results.next(); i++) {
            for (final var column : columns) {
                final var label = column.getColumnLabel();
                final var value = results.getObject(label);
                log.info("[{}] {}: {}", String.format("%02d", i), String.format(format, label), value);
            }
        }
    }

    private __ResultSetMetaDataColumnTestUtils() {
        throw new AssertionError("instantiation is not allowed");
    }
}
