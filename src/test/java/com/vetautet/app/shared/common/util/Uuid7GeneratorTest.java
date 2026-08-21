package com.vetautet.app.shared.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Uuid7GeneratorTest {

    @Test
    void generate_returnsVersion7Variant2Uuid() {
        UUID uuid = Uuid7Generator.generate();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generate_withExplicitTimestamp_roundTripsThroughExtractTimestamp() {
        Instant timestamp = Instant.parse("2025-01-15T10:30:00.123Z");

        UUID uuid = Uuid7Generator.generate(timestamp);

        assertThat(Uuid7Generator.extractTimestamp(uuid))
                .isEqualTo(timestamp.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void generate_successiveIncreasingTimestamps_produceIncreasingUuids() {
        Instant earlier = Instant.parse("2025-01-15T10:30:00.000Z");
        Instant later = Instant.parse("2025-01-15T10:30:00.500Z");

        UUID earlierUuid = Uuid7Generator.generate(earlier);
        UUID laterUuid = Uuid7Generator.generate(later);

        assertThat(earlierUuid).isLessThan(laterUuid);
    }

    @Test
    void generate_twoCallsSameInstant_produceDifferentRandomUuids() {
        Instant sameInstant = Instant.parse("2025-01-15T10:30:00.000Z");

        UUID first = Uuid7Generator.generate(sameInstant);
        UUID second = Uuid7Generator.generate(sameInstant);

        assertThat(first).isNotEqualTo(second);
    }
}
