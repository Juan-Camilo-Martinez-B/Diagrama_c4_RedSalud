package com.redsaludandina.scheduling.application.command;

import com.redsaludandina.scheduling.application.port.SlotRepository;
import com.redsaludandina.scheduling.domain.model.Slot;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

/** Reserva un cupo y deja {@code SlotBooked} en el outbox, en la misma transacción. */
public final class BookSlotHandler implements CommandHandler<BookSlotCommand> {

    private final SlotRepository slots;
    private final UnitOfWork unitOfWork;
    private final OutboxRecorder outbox;
    private final Clock clock;

    public BookSlotHandler(SlotRepository slots, UnitOfWork unitOfWork, OutboxRecorder outbox, Clock clock) {
        this.slots = Require.notNull(slots, "slots");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.outbox = Require.notNull(outbox, "outbox");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(BookSlotCommand command) {
        Require.notNull(command.slotId(), "slotId");
        unitOfWork.run(() -> {
            Slot slot = slots.findForUpdate(command.slotId())
                    .orElseThrow(() -> new ResourceNotFoundException("el cupo " + command.slotId()));
            slot.book(command.bookingId(), command.patientId(), command.centerId(), clock.instant());
            slots.save(slot);
            outbox.record(slot);
        });
    }
}
