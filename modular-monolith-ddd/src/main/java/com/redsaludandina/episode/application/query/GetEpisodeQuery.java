package com.redsaludandina.episode.application.query;

import com.redsaludandina.shared.application.cqrs.Query;

/**
 * Devuelve el episodio. No audita ni autoriza (las consultas no tienen efectos): la API ejecuta antes
 * {@code RecordEpisodeReadCommand}, que autoriza y deja la lectura registrada.
 */
public record GetEpisodeQuery(String episodeId) implements Query<EpisodeView> {
}
