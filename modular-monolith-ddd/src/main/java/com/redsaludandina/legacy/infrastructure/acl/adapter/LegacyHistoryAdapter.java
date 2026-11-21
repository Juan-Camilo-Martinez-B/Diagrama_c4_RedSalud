package com.redsaludandina.legacy.infrastructure.acl.adapter;

import com.redsaludandina.legacy.application.port.LegacyHistorySource;
import com.redsaludandina.legacy.application.port.LegacyUnavailableException;
import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import com.redsaludandina.legacy.infrastructure.acl.client.db.LegacyDbClient;
import com.redsaludandina.legacy.infrastructure.acl.translator.CanonicalTranslator;
import java.util.Optional;
import org.springframework.dao.DataAccessException;

/**
 * Adaptador de la capa anticorrupción: ÚNICA implementación de {@link LegacyHistorySource}. Consulta
 * el legado con su cliente y entrega el historial ya traducido; una falla del legado se convierte en
 * {@link LegacyUnavailableException}, que el núcleo sí entiende.
 */
public final class LegacyHistoryAdapter implements LegacyHistorySource {

    private final LegacyDbClient client;
    private final CanonicalTranslator translator;

    public LegacyHistoryAdapter(LegacyDbClient client, CanonicalTranslator translator) {
        this.client = client;
        this.translator = translator;
    }

    @Override
    public Optional<ClinicalHistory> fetch(String patientId, FreshnessMark freshness) {
        try {
            return client.fetch(translator.legacyCode(patientId)).map(legacy -> translator.translate(legacy, freshness));
        } catch (DataAccessException failure) {
            throw new LegacyUnavailableException("El legado no respondió: " + failure.getMessage(), failure);
        }
    }
}
