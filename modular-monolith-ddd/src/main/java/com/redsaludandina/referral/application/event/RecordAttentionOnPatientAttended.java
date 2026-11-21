package com.redsaludandina.referral.application.event;

import com.redsaludandina.referral.application.command.ReferralWriter;
import com.redsaludandina.referral.application.port.ReferralRepository;
import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.application.messaging.EventOutOfOrderException;
import com.redsaludandina.shared.application.messaging.InboxPort;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.PatientAttended;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Una teleconsulta terminada desde un cupo marca atendida la interconsulta de ese cupo. Si el
 * paciente tiene una interconsulta solicitada pero aún no llegó el {@code SlotBooked}, el evento llegó
 * fuera de orden: se rechaza para reintentarlo después.
 */
public final class RecordAttentionOnPatientAttended implements EventConsumer<PatientAttended> {

    public static final String NAME = "referral.record-attention";

    private final ReferralRepository referrals;
    private final ReferralWriter writer;
    private final InboxPort inbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RecordAttentionOnPatientAttended(ReferralRepository referrals, ReferralWriter writer, InboxPort inbox,
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
    public Class<PatientAttended> eventType() {
        return PatientAttended.class;
    }

    @Override
    public void consume(UUID eventId, PatientAttended event) {
        if (event.slotId() == null) {
            return;
        }
        unitOfWork.run(() -> {
            if (inbox.registerIfNew(NAME, eventId)) {
                recordAttention(event);
            }
        });
    }

    private void recordAttention(PatientAttended event) {
        Optional<Referral> referral = referrals.findBySlot(event.slotId());
        if (referral.isEmpty()) {
            rejectIfOutOfOrder(event);
            return;
        }
        referral.get().recordAttention(event.episodeId(), clock.instant());
        writer.save(referral.get());
    }

    private void rejectIfOutOfOrder(PatientAttended event) {
        if (referrals.oldestRequestedFor(event.patientId()).isPresent()) {
            throw new EventOutOfOrderException("PatientAttended del episodio " + event.episodeId()
                    + " llegó antes que el SlotBooked de su interconsulta");
        }
    }
}
