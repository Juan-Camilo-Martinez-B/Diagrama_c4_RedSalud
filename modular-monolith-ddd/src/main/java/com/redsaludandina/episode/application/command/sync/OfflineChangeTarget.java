package com.redsaludandina.episode.application.command.sync;

import com.redsaludandina.episode.application.command.AttachFileCommand;
import com.redsaludandina.episode.application.command.CloseTeleconsultationCommand;
import com.redsaludandina.episode.application.command.OpenEpisodeCommand;
import com.redsaludandina.episode.application.command.RecordCareCommand;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

/**
 * Los comandos del episodio que un cambio offline puede aplicar. La sincronización reutiliza
 * exactamente los mismos casos de uso que la API en línea: no hay reglas duplicadas.
 */
public record OfflineChangeTarget(CommandHandler<OpenEpisodeCommand> open,
                                  CommandHandler<RecordCareCommand> recordCare,
                                  CommandHandler<AttachFileCommand> attachFile,
                                  CommandHandler<CloseTeleconsultationCommand> close) {

    public OfflineChangeTarget {
        Require.notNull(open, "open");
        Require.notNull(recordCare, "recordCare");
        Require.notNull(attachFile, "attachFile");
        Require.notNull(close, "close");
    }
}
