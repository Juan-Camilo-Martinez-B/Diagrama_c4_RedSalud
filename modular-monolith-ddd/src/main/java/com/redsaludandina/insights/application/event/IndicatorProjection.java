package com.redsaludandina.insights.application.event;

import com.redsaludandina.insights.application.port.UsageIndicatorRepository;
import com.redsaludandina.insights.domain.analytics.UsageIndicator;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.application.messaging.InboxPort;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Proyección de un tipo de evento sobre el indicador del centro y mes en que ocurrió. El inbox se
 * registra en la misma transacción que el indicador: un evento repetido no cuenta dos veces.
 */
public final class IndicatorProjection<E extends DomainEvent> implements EventConsumer<E> {

    /** Cómo afecta un tipo de evento al indicador. */
    public record Rule<E>(String name, Class<E> eventType, Function<E, String> centerOf,
                          BiConsumer<UsageIndicator, E> apply) {
    }

    private final Rule<E> rule;
    private final UsageIndicatorRepository indicators;
    private final InboxPort inbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public IndicatorProjection(Rule<E> rule, UsageIndicatorRepository indicators, InboxPort inbox,
                               UnitOfWork unitOfWork, Clock clock) {
        this.rule = Require.notNull(rule, "rule");
        this.indicators = Require.notNull(indicators, "indicators");
        this.inbox = Require.notNull(inbox, "inbox");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public String name() {
        return rule.name();
    }

    @Override
    public Class<E> eventType() {
        return rule.eventType();
    }

    @Override
    public void consume(UUID eventId, E event) {
        unitOfWork.run(() -> {
            if (inbox.registerIfNew(rule.name(), eventId)) {
                project(event);
            }
        });
    }

    private void project(E event) {
        String centerId = rule.centerOf().apply(event);
        YearMonth period = YearMonth.from(event.occurredAt().atZone(clock.getZone()));
        UsageIndicator indicator = indicators.find(centerId, period)
                .orElseGet(() -> UsageIndicator.start(centerId, period));
        rule.apply().accept(indicator, event);
        indicators.save(indicator);
    }
}
