package com.redsaludandina.episode.application.command;

import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.identity.application.command.AuthorizeAccessCommand;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

/**
 * Pide a Identidad (OHS) que autorice el acceso con los atributos del episodio: paciente y centro. Se
 * ejecuta fuera de cualquier transacción del episodio para que una negación quede registrada.
 */
public final class AuthorizeEpisodeAccessHandler implements CommandHandler<AuthorizeEpisodeAccessCommand> {

    private final EpisodeUpdater updater;
    private final CommandHandler<AuthorizeAccessCommand> authorizeAccess;

    public AuthorizeEpisodeAccessHandler(EpisodeUpdater updater, CommandHandler<AuthorizeAccessCommand> authorizeAccess) {
        this.updater = Require.notNull(updater, "updater");
        this.authorizeAccess = Require.notNull(authorizeAccess, "authorizeAccess");
    }

    @Override
    public void handle(AuthorizeEpisodeAccessCommand command) {
        Episode episode = updater.read(command.episodeId());
        authorizeAccess.handle(new AuthorizeAccessCommand(command.userId(), episode.resource(), episode.patientId(),
                episode.centerId(), null));
    }
}
