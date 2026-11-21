package com.redsaludandina.episode.application.command;

import com.redsaludandina.episode.application.port.AttachmentStorage;
import com.redsaludandina.episode.domain.model.Attachment;
import com.redsaludandina.episode.domain.model.AttachmentType;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

/** Adjunta el archivo solo si su clave es la que el servidor asignó (nadie apunta a objetos ajenos). */
public final class AttachFileHandler implements CommandHandler<AttachFileCommand> {

    private final EpisodeUpdater updater;
    private final AttachmentStorage storage;

    public AttachFileHandler(EpisodeUpdater updater, AttachmentStorage storage) {
        this.updater = Require.notNull(updater, "updater");
        this.storage = Require.notNull(storage, "storage");
    }

    @Override
    public void handle(AttachFileCommand command) {
        String expectedKey = storage.keyFor(command.episodeId(), Require.notNull(command.attachmentId(), "attachmentId"));
        if (!expectedKey.equals(command.storageKey())) {
            throw new IllegalArgumentException("La clave del adjunto debe ser " + expectedKey);
        }
        Attachment attachment = new Attachment(command.attachmentId(), AttachmentType.parse(command.type()),
                expectedKey, command.sha256());
        updater.update(command.episodeId(), episode -> episode.attach(attachment));
    }
}
