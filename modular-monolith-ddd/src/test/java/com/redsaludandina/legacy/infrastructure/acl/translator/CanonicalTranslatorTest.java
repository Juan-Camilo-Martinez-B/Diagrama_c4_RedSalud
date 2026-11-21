package com.redsaludandina.legacy.infrastructure.acl.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import com.redsaludandina.legacy.domain.model.HistorySource;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory.LegacyAttention;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory.LegacyDocument;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalTranslatorTest {

    private static final FreshnessMark FRESH =
            FreshnessMark.withTtl(Instant.parse("2026-03-01T10:00:00Z"), Duration.ofHours(24), HistorySource.DIRECT_LEGACY);
    private final CanonicalTranslator translator = new CanonicalTranslator();

    @Test
    void translatesTheLegacyModelToTheCanonicalOne() {
        LegacyHistory legacy = new LegacyHistory(" pac-1 ",
                List.of(new LegacyAttention("AT-1", "20240301", "Control"), new LegacyAttention("AT-2", "20250709", " ")),
                List.of(new LegacyDocument("DOC-1", "lab", "20250709"), new LegacyDocument("DOC-2", "ZZ", "20240301")));

        ClinicalHistory history = translator.translate(legacy, FRESH);

        assertThat(history.patientId()).isEqualTo("pac-1");
        assertThat(history.episodes().get(0).date()).isEqualTo(LocalDate.of(2025, 7, 9));
        assertThat(history.episodes().get(0).description()).isEqualTo("(sin glosa)");
        assertThat(history.documents()).extracting(document -> document.type()).containsExactly("Laboratorio", "Otro (ZZ)");
    }

    @Test
    void anInvalidLegacyDateIsReported() {
        LegacyHistory legacy = new LegacyHistory("pac-1", List.of(new LegacyAttention("AT-1", "2024-03", "x")), List.of());

        assertThatThrownBy(() -> translator.translate(legacy, FRESH)).isInstanceOf(IllegalStateException.class);
    }
}
