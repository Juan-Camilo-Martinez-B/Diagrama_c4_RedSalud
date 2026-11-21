package com.redsaludandina.scheduling.domain.model;

import static com.redsaludandina.TestIds.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.event.SlotBooked;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SlotTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final TimeWindow TOMORROW =
            new TimeWindow(Instant.parse("2026-03-02T10:00:00Z"), Instant.parse("2026-03-02T10:30:00Z"));

    @Test
    void bookingAFreeSlotPublishesSlotBooked() {
        Slot slot = Slot.publish(id(1), "esp-1", TOMORROW);

        slot.book(id(2), "pac-1", "centro-a", NOW);

        assertThat(slot.status()).isEqualTo(SlotStatus.BOOKED);
        assertThat(slot.pullEvents()).singleElement().isInstanceOfSatisfying(SlotBooked.class,
                event -> assertThat(event.slotStart()).isEqualTo(TOMORROW.start()));
    }

    @Test
    void bookingAgainWithTheSameBookingIsIdempotent() {
        Slot slot = Slot.publish(id(1), "esp-1", TOMORROW);
        slot.book(id(2), "pac-1", "centro-a", NOW);
        slot.pullEvents();

        slot.book(id(2), "pac-1", "centro-a", NOW);

        assertThat(slot.pullEvents()).isEmpty();
    }

    @Test
    void doubleBookingIsRejected() {
        Slot slot = Slot.publish(id(1), "esp-1", TOMORROW);
        slot.book(id(2), "pac-1", "centro-a", NOW);

        assertThatThrownBy(() -> slot.book(id(3), "pac-2", "centro-a", NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo(Slot.SLOT_ALREADY_BOOKED));
    }

    @Test
    void aSlotThatAlreadyStartedCannotBeBooked() {
        Slot slot = Slot.publish(id(1), "esp-1", TOMORROW);

        assertThatThrownBy(() -> slot.book(id(2), "pac-1", "centro-a", TOMORROW.start()))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo(Slot.SLOT_ALREADY_STARTED));
    }
}
