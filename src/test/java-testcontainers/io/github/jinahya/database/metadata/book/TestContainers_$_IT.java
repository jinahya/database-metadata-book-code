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
import io.vavr.CheckedFunction1;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

// https://java.testcontainers.org/modules/databases/jdbc/

@Testcontainers
@Slf4j
abstract class TestContainers_$_IT
        extends _Metadata_Test {

    @BeforeAll
    static void checkDocker() throws IOException, InterruptedException {
        log.info("checking docker...");
        final var process = new ProcessBuilder()
                .command("docker", "images")
                .start();
        final int exitValue = process.waitFor();
        log.info("exitValue: {}", exitValue);
        assumeTrue(exitValue == 0);
    }

    abstract Connection connect() throws SQLException;

    // ------------------------------------------------------------------------------------------------------ connection
    <R> R applyConnection(final CheckedFunction1<? super Connection, ? extends R> function) throws Throwable {
        return __JavaSqlTestUtils.applyConnection(this::connect, function);
    }

    // --------------------------------------------------------------------------------------------------------- context
    <R> R applyContext(final CheckedFunction1<? super Context, ? extends R> function) throws Throwable {
        return applyConnection(c -> function.apply(Context.newInstance(c)));
    }
}

