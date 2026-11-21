package com.redsaludandina.episode.application.command;

import com.redsaludandina.episode.domain.model.CareField;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.CenterStamp;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

/** Aplica last-writer-wins sobre el campo; si descarta un valor, el evento queda en el outbox. */
public final class RecordCareHandler implements CommandHandler<RecordCareCommand> {

    private final EpisodeUpdater updater;
    private final Clock clock;

    public RecordCareHandler(EpisodeUpdater updater, Clock clock) {
        this.updater = Require.notNull(updater, "updater");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(RecordCareCommand command) {
        CareField field = CareField.parse(command.field());
        CenterStamp stamp = new CenterStamp(command.centerId(), command.stampedAt());
        updater.update(command.episodeId(), episode -> episode.recordCare(field, command.text(), stamp, clock.instant()));
    }
}
