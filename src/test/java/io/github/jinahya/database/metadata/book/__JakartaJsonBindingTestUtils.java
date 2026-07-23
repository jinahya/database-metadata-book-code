package io.github.jinahya.database.metadata.book;

import com.github.jinahya.database.metadata.bind.MetadataType;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

@Slf4j
final class __JakartaJsonBindingTestUtils {

    /**
     * Serializes the specified metadata types to a formatted JSON array and logs it.
     *
     * @param types the metadata types to print.
     */
    static void print(final Collection<? extends MetadataType> types) {
        requireNonNull(types, "types is null");
        try (var jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            log.info("{}", jsonb.toJson(types));
        } catch (final Exception e) {
            throw new RuntimeException("failed to print metadata types as JSON", e);
        }
    }

    private __JakartaJsonBindingTestUtils() {
        throw new AssertionError("instantiation is not allowed");
    }
}
