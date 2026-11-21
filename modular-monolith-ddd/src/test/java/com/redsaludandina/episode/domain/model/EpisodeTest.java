package com.redsaludandina.episode.domain.model;

import static com.redsaludandina.TestIds.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.CenterStamp;
import com.redsaludandina.shared.event.ClinicalRead;
import com.redsaludandina.shared.event.ClinicalValueDiscarded;
import com.redsaludandina.shared.event.PatientAttended;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EpisodeTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final CenterStamp EARLY = new CenterStamp("centro-a", Instant.parse("2026-03-01T08:00:00Z"));
    private static final CenterStamp LATE = new CenterStamp("centro-b", Instant.parse("2026-03-01T09:00:00Z"));
    private static final String SHA = "b".repeat(64);

    private static Episode open() {
        return Episode.open(id(1), "pac-1", id(9), "centro-a");
    }

    @Test
    void lastWriterWinsAndTheDiscardedValueIsPublished() {
        Episode episode = open();
        episode.recordCare(CareField.FINDINGS, "38,5", LATE, NOW);

        episode.recordCare(CareField.FINDINGS, "37,9", EARLY, NOW);

        assertThat(episode.care().value(CareField.FINDINGS).orElseThrow().text()).isEqualTo("38,5");
        assertThat(episode.pullEvents()).singleElement().isInstanceOfSatisfying(ClinicalValueDiscarded.class, event -> {
            assertThat(event.keptValue()).isEqualTo("38,5");
            assertThat(event.discardedValue()).isEqualTo("37,9");
        });
    }

    @Test
    void aLaterValueReplacesTheCurrentOne() {
        Episode episode = open();
        episode.recordCare(CareField.FINDINGS, "37,9", EARLY, NOW);

        episode.recordCare(CareField.FINDINGS, "38,5", LATE, NOW);

        assertThat(episode.care().value(CareField.FINDINGS).orElseThrow().stamp()).isEqualTo(LATE);
    }

    @Test
    void resendingTheSameValueChangesNothing() {
        Episode episode = open();
        episode.recordCare(CareField.REASON, "Fiebre", EARLY, NOW);

        episode.recordCare(CareField.REASON, "Fiebre", EARLY, NOW);

        assertThat(episode.pullEvents()).isEmpty();
    }

    @Test
    void closingRequiresAReasonAndPublishesPatientAttendedOnce() {
        Episode episode = open();
        assertThatThrownBy(() -> episode.markAttended(NOW)).isInstanceOfSatisfying(BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo(Episode.CARE_WITHOUT_REASON));

        episode.recordCare(CareField.REASON, "Fiebre", EARLY, NOW);
        episode.markAttended(NOW);
        episode.markAttended(NOW);

        assertThat(episode.status()).isEqualTo(EpisodeStatus.ATTENDED);
        assertThat(episode.pullEvents()).singleElement().isInstanceOf(PatientAttended.class);
    }

    @Test
    void reattachingTheSameFileIsIdempotentButOtherContentConflicts() {
        Episode episode = open();
        Attachment exam = new Attachment(id(5), AttachmentType.EXAM, "k", SHA);
        episode.attach(exam);
        episode.attach(exam);

        assertThat(episode.attachments()).hasSize(1);
        assertThatThrownBy(() -> episode.attach(new Attachment(id(5), AttachmentType.EXAM, "k", "c".repeat(64))))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo(Episode.ATTACHMENT_CONFLICT));
    }

    @Test
    void everyReadIsRecordedWithItsReason() {
        Episode episode = open();

        episode.recordRead("med-1", "Control", NOW);

        assertThat(episode.pullEvents()).singleElement().isInstanceOfSatisfying(ClinicalRead.class,
                event -> assertThat(event.resource()).isEqualTo("episode/" + id(1)));
    }
}
