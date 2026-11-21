package com.redsaludandina.scheduling.application.command;

import com.redsaludandina.scheduling.application.port.SlotRepository;
import com.redsaludandina.scheduling.domain.model.Slot;
import com.redsaludandina.scheduling.domain.model.TimeWindow;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

/** Publica un cupo. Repetir el comando con el mismo id no crea otro cupo. */
public final class PublishSlotHandler implements CommandHandler<PublishSlotCommand> {

    private final SlotRepository slots;
    private final UnitOfWork unitOfWork;

    public PublishSlotHandler(SlotRepository slots, UnitOfWork unitOfWork) {
        this.slots = Require.notNull(slots, "slots");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
    }

    @Override
    public void handle(PublishSlotCommand command) {
        Slot slot = Slot.publish(command.slotId(), command.specialistId(), new TimeWindow(command.start(), command.end()));
        unitOfWork.run(() -> {
            if (!slots.exists(slot.id())) {
                slots.save(slot);
            }
        });
    }
}
