package com.redsaludandina.referral.application.event;

import com.redsaludandina.referral.application.command.ReferralWriter;
import com.redsaludandina.referral.application.port.ReferralRepository;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.application.messaging.InboxPort;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.SlotBooked;
import java.time.Clock;
import java.util.UUID;

/**
 * Un cupo reservado agenda la interconsulta de ese cupo o, si aún no tiene cupo, la SOLICITADA más
 * antigua del paciente. Si el paciente no tiene interconsultas, el evento no aplica.
 */
public final class ScheduleReferralOnSlotBooked implements EventConsumer<SlotBooked> {

    public static final String NAME = "referral.schedule";

    private final ReferralRepository referrals;
    private final ReferralWriter writer;
    private final InboxPort inbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ScheduleReferralOnSlotBooked(ReferralRepository referrals, ReferralWriter writer, InboxPort inbox,
                                        UnitOfWork unitOfWork, Clock clock) {
        this.referrals = Require.notNull(referrals, "referrals");
        this.writer = Require.notNull(writer, "writer");
        this.inbox = Require.notNull(inbox, "inbox");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<SlotBooked> eventType() {
        return SlotBooked.class;
    }

    @Override
    public void consume(UUID eventId, SlotBooked event) {
        unitOfWork.run(() -> {
            if (inbox.registerIfNew(NAME, eventId)) {
                schedule(event);
            }
        });
    }

    private void schedule(SlotBooked event) {
        referrals.findBySlot(event.slotId())
                .or(() -> referrals.oldestRequestedFor(event.patientId()))
                .ifPresent(referral -> {
                    referral.schedule(event.slotId(), clock.instant());
                    writer.save(referral);
                });
    }
}
