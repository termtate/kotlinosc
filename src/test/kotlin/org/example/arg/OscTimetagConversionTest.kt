package org.example.arg

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class OscTimetagConversionTest {
    @Test
    fun `instant timetag round trip within precision`() {
        val samples = listOf(
            Instant.fromEpochSeconds(0, 0),
            Instant.fromEpochSeconds(1_700_000_000, 123_456_789),
            Instant.fromEpochSeconds(-1_000_000_000, 987_654_321),
            Instant.fromEpochSeconds(2_085_978_495, 999_999_999),
            Instant.fromEpochSeconds(-2_208_988_800, 0)
        )

        for (instant in samples) {
            val roundTrip = instant.toOscTimetag().toInstant()
            assertTrue(
                abs(asEpochNanos(roundTrip) - asEpochNanos(instant)) <= 1L,
                "Round-trip precision drift is too large for $instant: got $roundTrip"
            )
        }
    }

    @Test
    fun `instant to timetag throws when out of range`() {
        assertFailsWith<IllegalArgumentException> {
            Instant.fromEpochSeconds(-2_208_988_801, 0).toOscTimetag()
        }
        assertFailsWith<IllegalArgumentException> {
            Instant.fromEpochSeconds(2_085_978_496, 0).toOscTimetag()
        }
    }

    @Test
    fun `long to timetag throws when negative`() {
        assertFailsWith<IllegalArgumentException> {
            (-1L).toOscTimetag()
        }
    }

    @Test
    fun `long to timetag keeps value when non negative`() {
        assertEquals(0uL, 0L.toOscTimetag().value)
        assertEquals(123uL, 123L.toOscTimetag().value)
        assertEquals(Long.MAX_VALUE.toULong(), Long.MAX_VALUE.toOscTimetag().value)
    }

    private fun asEpochNanos(instant: Instant): Long {
        return instant.epochSeconds * 1_000_000_000L + instant.nanosecondsOfSecond
    }
}
