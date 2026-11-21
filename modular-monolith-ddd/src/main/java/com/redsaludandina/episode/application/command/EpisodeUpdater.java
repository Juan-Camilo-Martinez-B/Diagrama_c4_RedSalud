package com.redsaludandina.episode.application.command;

import com.redsaludandina.episode.application.port.EpisodeRepository;
import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import java.util.function.Consumer;

/**
 * Plantilla común de los comandos que modifican un episodio: en UNA transacción carga el agregado,
 * aplica el cambio, lo guarda y pasa sus eventos al outbox.
 */
public final class EpisodeUpdater {

    private final EpisodeRepository episodes;
    private final UnitOfWork unitOfWork;
    private final OutboxRecorder outbox;

    public EpisodeUpdater(EpisodeRepository episodes, UnitOfWork unitOfWork, OutboxRecorder outbox) {
        this.episodes = Require.notNull(episodes, "episodes");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.outbox = Require.notNull(outbox, "outbox");
    }

    public void update(ClinicalId episodeId, Consumer<Episode> change) {
        unitOfWork.run(() -> {
            Episode episode = load(episodeId);
            change.accept(episode);
            episodes.save(episode);
            outbox.record(episode);
        });
    }

    /** Lee el agregado en su propia transacción (por ejemplo, para autorizar antes de modificar). */
    public Episode read(ClinicalId episodeId) {
        return unitOfWork.call(() -> load(episodeId));
    }

    private Episode load(ClinicalId episodeId) {
        return episodes.findById(Require.notNull(episodeId, "episodeId"))
                .orElseThrow(() -> new ResourceNotFoundException("el episodio " + episodeId));
    }
}
