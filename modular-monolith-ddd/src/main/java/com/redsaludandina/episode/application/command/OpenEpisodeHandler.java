package com.redsaludandina.episode.application.command;

import com.redsaludandina.episode.application.port.EpisodeRepository;
import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

/** Abrir dos veces el mismo episodio (mismo ULID) no lo duplica: es idempotente. */
public final class OpenEpisodeHandler implements CommandHandler<OpenEpisodeCommand> {

    private final EpisodeRepository episodes;
    private final UnitOfWork unitOfWork;

    public OpenEpisodeHandler(EpisodeRepository episodes, UnitOfWork unitOfWork) {
        this.episodes = Require.notNull(episodes, "episodes");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
    }

    @Override
    public void handle(OpenEpisodeCommand command) {
        Episode episode = Episode.open(command.episodeId(), command.patientId(), command.slotId(), command.centerId());
        unitOfWork.run(() -> {
            if (!episodes.exists(episode.id())) {
                episodes.save(episode);
            }
        });
    }
}
