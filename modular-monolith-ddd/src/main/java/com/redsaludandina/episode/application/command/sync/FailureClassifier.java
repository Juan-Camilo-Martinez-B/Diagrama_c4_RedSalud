package com.redsaludandina.episode.application.command.sync;

import com.redsaludandina.episode.domain.sync.ItemResult;
import com.redsaludandina.shared.application.DuplicateRecordException;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.domain.BusinessRuleException;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Traduce por qué falló un cambio en su resultado. Es una tabla ordenada (de lo más específico a lo
 * más general) en vez de una cadena de {@code catch}: agregar un caso es agregar una fila.
 */
final class FailureClassifier {

    private record Rule(Class<? extends RuntimeException> type, BiFunction<String, RuntimeException, ItemResult> result) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(DuplicateRecordException.class, (changeId, failure) -> ItemResult.alreadyApplied(changeId)),
            new Rule(BusinessRuleException.class, (changeId, failure) ->
                    ItemResult.rejected(changeId, ((BusinessRuleException) failure).code(), failure.getMessage())),
            new Rule(ResourceNotFoundException.class, (changeId, failure) ->
                    ItemResult.pending(changeId, ItemResult.UNAVAILABLE, failure.getMessage())),
            new Rule(IllegalArgumentException.class, (changeId, failure) ->
                    ItemResult.rejected(changeId, ItemResult.INVALID_DATA, failure.getMessage())));

    private FailureClassifier() {
    }

    /** Cualquier otra falla se considera transitoria: el cambio queda pendiente para reintentar. */
    static ItemResult classify(String changeId, RuntimeException failure) {
        return RULES.stream()
                .filter(rule -> rule.type().isInstance(failure))
                .findFirst()
                .map(rule -> rule.result().apply(changeId, failure))
                .orElseGet(() -> ItemResult.pending(changeId, ItemResult.UNAVAILABLE, "No se pudo aplicar; reintente"));
    }
}
