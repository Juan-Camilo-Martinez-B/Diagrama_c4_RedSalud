package com.redsaludandina.insights.domain.analytics;

import static com.redsaludandina.TestIds.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.shared.event.ReferralRequested;
import com.redsaludandina.shared.event.SlotBooked;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class UsageIndicatorTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Test
    void averageWaitAndClosureRate() {
        UsageIndicator indicator = UsageIndicator.start("centro-a", YearMonth.of(2026, 3));

        indicator.onSlotBooked(new SlotBooked(id(1), id(2), "pac-1", "centro-a", "esp-1", NOW.plus(Duration.ofHours(2)), NOW));
        indicator.onReferralRequested(new ReferralRequested(id(3), "centro-a", "Cardiología", NOW));
        indicator.onReferralRequested(new ReferralRequested(id(4), "centro-a", "Cardiología", NOW));
        indicator.onReferralClosed(new ReferralClosed(id(3), "centro-a", NOW));

        assertThat(indicator.averageWait()).contains(Duration.ofHours(2));
        assertThat(indicator.closureRate()).contains(0.5);
    }

    @Test
    void withoutDataTheAveragesAreUnknown() {
        UsageIndicator indicator = UsageIndicator.start("centro-a", YearMonth.of(2026, 3));

        assertThat(indicator.averageWait()).isEmpty();
        assertThat(indicator.closureRate()).isEmpty();
    }

    @Test
    void anEventOfAnotherCenterIsRejected() {
        UsageIndicator indicator = UsageIndicator.start("centro-a", YearMonth.of(2026, 3));

        assertThatThrownBy(() -> indicator.onReferralClosed(new ReferralClosed(id(3), "centro-b", NOW)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
