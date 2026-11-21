package com.redsaludandina.episode.application.query;

import com.redsaludandina.episode.application.port.AttachmentStorage;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;

/** Firmar una URL no cambia estado: solo calcula una firma con vencimiento. */
public final class GetAttachmentUploadLinkHandler implements QueryHandler<GetAttachmentUploadLinkQuery, UploadLink> {

    private final AttachmentStorage storage;

    public GetAttachmentUploadLinkHandler(AttachmentStorage storage) {
        this.storage = Require.notNull(storage, "storage");
    }

    @Override
    public UploadLink handle(GetAttachmentUploadLinkQuery query) {
        return storage.presignUpload(Require.notNull(query.episodeId(), "episodeId"),
                Require.notNull(query.attachmentId(), "attachmentId"), Require.text(query.sha256(), "sha256"));
    }
}
