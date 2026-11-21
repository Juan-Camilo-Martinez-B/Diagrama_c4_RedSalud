package com.redsaludandina.episode.application.query;

import com.redsaludandina.episode.application.port.EpisodeReadModel;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;

public final class GetEpisodeHandler implements QueryHandler<GetEpisodeQuery, EpisodeView> {

    private final EpisodeReadModel readModel;

    public GetEpisodeHandler(EpisodeReadModel readModel) {
        this.readModel = Require.notNull(readModel, "readModel");
    }

    @Override
    public EpisodeView handle(GetEpisodeQuery query) {
        String episodeId = Require.text(query.episodeId(), "episodeId");
        return readModel.findById(episodeId).orElseThrow(() -> new ResourceNotFoundException("el episodio " + episodeId));
    }
}
