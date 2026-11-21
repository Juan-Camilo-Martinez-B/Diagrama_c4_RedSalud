package com.redsaludandina.episode.application.query;

import com.redsaludandina.episode.application.port.AttachmentStorage;
import com.redsaludandina.episode.application.port.EpisodeReadModel;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;

public final class GetAttachmentDownloadLinkHandler implements QueryHandler<GetAttachmentDownloadLinkQuery, DownloadLink> {

    private final EpisodeReadModel readModel;
    private final AttachmentStorage storage;

    public GetAttachmentDownloadLinkHandler(EpisodeReadModel readModel, AttachmentStorage storage) {
        this.readModel = Require.notNull(readModel, "readModel");
        this.storage = Require.notNull(storage, "storage");
    }

    @Override
    public DownloadLink handle(GetAttachmentDownloadLinkQuery query) {
        String storageKey = readModel.attachmentStorageKey(query.episodeId(), query.attachmentId())
                .orElseThrow(() -> new ResourceNotFoundException("el adjunto " + query.attachmentId()));
        return storage.presignDownload(storageKey);
    }
}
