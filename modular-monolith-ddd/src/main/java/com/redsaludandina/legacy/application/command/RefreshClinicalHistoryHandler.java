package com.redsaludandina.legacy.application.command;

import com.redsaludandina.legacy.application.port.HistoryReadModelWriter;
import com.redsaludandina.legacy.application.port.LegacyHistorySource;
import com.redsaludandina.legacy.application.port.LegacyUnavailableException;
import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import com.redsaludandina.legacy.domain.model.HistorySource;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Copia vigente → no toca el legado (p95 ≤ 2 s). Copia caducada o ausente → consulta el legado
 * (límite 6 s). Si el legado no responde pero hay una copia caducada, se conserva esa copia: la
 * consulta la mostrará marcada como no vigente.
 */
public final class RefreshClinicalHistoryHandler implements CommandHandler<RefreshClinicalHistoryCommand> {

    private final HistoryReadModelWriter readModel;
    private final LegacyHistorySource legacy;
    private final Duration ttl;
    private final Clock clock;

    public RefreshClinicalHistoryHandler(HistoryReadModelWriter readModel, LegacyHistorySource legacy, Duration ttl,
                                         Clock clock) {
        this.readModel = Require.notNull(readModel, "readModel");
        this.legacy = Require.notNull(legacy, "legacy");
        this.ttl = Require.notNull(ttl, "ttl");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(RefreshClinicalHistoryCommand command) {
        String patientId = Require.text(command.patientId(), "patientId");
        Instant now = clock.instant();
        Optional<FreshnessMark> current = readModel.freshnessOf(patientId);
        if (current.filter(mark -> mark.isFresh(now)).isEmpty()) {
            refresh(patientId, current.isPresent(), now);
        }
    }

    private void refresh(String patientId, boolean hasStaleCopy, Instant now) {
        try {
            ClinicalHistory fresh = legacy.fetch(patientId, FreshnessMark.withTtl(now, ttl, HistorySource.DIRECT_LEGACY))
                    .orElseThrow(() -> new ResourceNotFoundException("el historial de " + patientId));
            readModel.save(fresh);
        } catch (LegacyUnavailableException unavailable) {
            if (!hasStaleCopy) {
                throw unavailable;
            }
        }
    }
}
