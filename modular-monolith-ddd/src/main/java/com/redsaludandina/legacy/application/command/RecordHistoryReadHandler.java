package com.redsaludandina.legacy.application.command;

import com.redsaludandina.identity.application.command.AuthorizeAccessCommand;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.ClinicalRead;
import java.time.Clock;
import java.util.List;

/** Pide a Identidad que autorice y, si puede, deja {@code ClinicalRead} en el outbox de Legado. */
public final class RecordHistoryReadHandler implements CommandHandler<RecordHistoryReadCommand> {

    private final CommandHandler<AuthorizeAccessCommand> authorizeAccess;
    private final UnitOfWork unitOfWork;
    private final OutboxRecorder outbox;
    private final Clock clock;

    public RecordHistoryReadHandler(CommandHandler<AuthorizeAccessCommand> authorizeAccess, UnitOfWork unitOfWork,
                                    OutboxRecorder outbox, Clock clock) {
        this.authorizeAccess = Require.notNull(authorizeAccess, "authorizeAccess");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.outbox = Require.notNull(outbox, "outbox");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(RecordHistoryReadCommand command) {
        String reason = Require.text(command.reason(), "motivo de la lectura");
        String resource = "history/" + Require.text(command.patientId(), "patientId");
        authorizeAccess.handle(new AuthorizeAccessCommand(command.userId(), resource, command.patientId(),
                command.centerId(), null));
        unitOfWork.run(() -> outbox.record(resource,
                List.of(new ClinicalRead(command.userId(), resource, reason, clock.instant()))));
    }
}
