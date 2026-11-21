package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

public final class CloseTeleconsultationHandler implements CommandHandler<CloseTeleconsultationCommand> {

    private final EpisodeUpdater updater;
    private final Clock clock;

    public CloseTeleconsultationHandler(EpisodeUpdater updater, Clock clock) {
        this.updater = Require.notNull(updater, "updater");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(CloseTeleconsultationCommand command) {
        updater.update(command.episodeId(), episode -> episode.markAttended(clock.instant()));
    }
}
