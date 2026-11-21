package com.redsaludandina.referral.domain.model;

import static com.redsaludandina.TestIds.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.referral.domain.model.Referral.ReferralData;
import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.shared.event.ReferralRequested;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReferralTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    private static Referral requested() {
        return Referral.request(new ReferralData(id(1), id(2), "pac-1", "centro-a", "Cardiología", "Dolor", NOW));
    }

    private static AlertType pendingAlert(Referral referral) {
        return referral.pendingAlerts().get(0).type();
    }

    @Test
    void theLifecycleOpensOneAlertPerWaitingStep() {
        Referral referral = requested();
        assertThat(referral.pullEvents()).singleElement().isInstanceOf(ReferralRequested.class);
        assertThat(pendingAlert(referral)).isEqualTo(AlertType.PENDING_SCHEDULING);

        referral.schedule(id(3), NOW);
        assertThat(pendingAlert(referral)).isEqualTo(AlertType.PENDING_ATTENTION);

        referral.recordAttention(id(4), NOW);
        assertThat(pendingAlert(referral)).isEqualTo(AlertType.PENDING_CLOSURE);

        referral.close(NOW);
        assertThat(referral.pendingAlerts()).isEmpty();
        assertThat(referral.pullEvents()).singleElement().isInstanceOf(ReferralClosed.class);
    }

    @Test
    void theSameFactTwiceDoesNotAdvanceTwice() {
        Referral referral = requested();
        referral.schedule(id(3), NOW);

        referral.schedule(id(3), NOW);

        assertThat(referral.status()).isEqualTo(ReferralStatus.SCHEDULED);
    }

    @Test
    void stepsCannotBeSkipped() {
        Referral referral = requested();

        assertThatThrownBy(() -> referral.close(NOW)).isInstanceOfSatisfying(BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo(Referral.INVALID_TRANSITION));
    }
}
