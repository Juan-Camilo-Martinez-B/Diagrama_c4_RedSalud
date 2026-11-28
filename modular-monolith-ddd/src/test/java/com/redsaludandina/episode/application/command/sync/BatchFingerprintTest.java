package com.redsaludandina.episode.application.command.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La huella sale del toString del dominio y no cambia con el orden ni con la instancia. */
class BatchFingerprintTest {

    private static final Instant STAMP = Instant.parse("2026-03-01T08:00:00Z");

    @Test
    void orderDoesNotChangeTheFingerprint() {
        OfflineItem first = item("c1", 1, new OfflineChange.OpenEpisode("ep", "pac", null));
        OfflineItem second = item("c2", 2, new OfflineChange.RecordCare("ep", "REASON", "a|b", STAMP));

        assertThat(BatchFingerprint.of("centro-a", List.of(second, first)))
                .isEqualTo(BatchFingerprint.of("centro-a", List.of(first, second)));
    }

    @Test
    void anotherValueChangesTheFingerprint() {
        OfflineItem item = item("c1", 1, new OfflineChange.OpenEpisode("ep", "pac", null));
        OfflineItem other = item("c1", 1, new OfflineChange.OpenEpisode("ep", "otro", null));

        assertThat(BatchFingerprint.of("centro-a", List.of(item)))
                .isNotEqualTo(BatchFingerprint.of("centro-a", List.of(other)));
    }

    @Test
    void toStringIsTheDomainContract() {
        OfflineChange open = new OfflineChange.OpenEpisode("ep", "pac", null);
        OfflineChange care = new OfflineChange.RecordCare("ep", "REASON", "a|b\n", STAMP);

        assertThat(open.toString()).isEqualTo("OPEN_EPISODE|ep|pac|");
        assertThat(care.toString()).isEqualTo("RECORD_CARE|ep|REASON|a\\|b\\n|2026-03-01T08:00:00Z");
        assertThat(open.toString()).doesNotContain("@");
    }

    private static OfflineItem item(String changeId, long sequence, OfflineChange change) {
        return new OfflineItem(changeId, "agg", sequence, change);
    }
}
