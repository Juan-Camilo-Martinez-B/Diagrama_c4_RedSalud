package com.redsaludandina.episode.application.command;

import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.util.function.Consumer;

/** Autoriza la lectura y registra {@code ClinicalRead} en el outbox del episodio. */
public final class RecordEpisodeReadHandler implements CommandHandler<RecordEpisodeReadCommand> {

    private final EpisodeUpdater updater;
    private final CommandHandler<AuthorizeEpisodeAccessCommand> authorize;
    private final Clock clock;

    public RecordEpisodeReadHandler(EpisodeUpdater updater, CommandHandler<AuthorizeEpisodeAccessCommand> authorize,
                                    Clock clock) {
        this.updater = Require.notNull(updater, "updater");
        this.authorize = Require.notNull(authorize, "authorize");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(RecordEpisodeReadCommand command) {
        String reason = Require.text(command.reason(), "motivo de la lectura");
        authorize.handle(new AuthorizeEpisodeAccessCommand(command.episodeId(), command.userId()));
        updater.update(command.episodeId(), readOf(command, reason));
    }

    private Consumer<Episode> readOf(RecordEpisodeReadCommand command, String reason) {
        return command.attachmentId() == null
                ? episode -> episode.recordRead(command.userId(), reason, clock.instant())
                : episode -> episode.recordAttachmentRead(command.attachmentId(), command.userId(), reason, clock.instant());
    }
}
