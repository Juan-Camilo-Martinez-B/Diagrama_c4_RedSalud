package com.redsaludandina.shared.infrastructure.messaging;

import com.redsaludandina.shared.application.messaging.OutboxRelay;
import com.redsaludandina.shared.application.messaging.OutboxRelay.RelayResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Ejecuta periódicamente el relevo de los outbox de todos los contextos. */
public class ScheduledOutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledOutboxRelay.class);

    private final List<OutboxRelay> relays;
    private final boolean automatic;
    private final int batchSize;

    public ScheduledOutboxRelay(List<OutboxRelay> relays, boolean automatic, int batchSize) {
        this.relays = List.copyOf(relays);
        this.automatic = automatic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${redsalud.outbox.interval:PT2S}")
    public void scheduled() {
        if (automatic) {
            relayAll();
        }
    }

    /** Releva todos los outbox una vez y devuelve cuántos mensajes se publicaron. */
    public int relayAll() {
        return relays.stream().mapToInt(this::relayOne).sum();
    }

    private int relayOne(OutboxRelay relay) {
        RelayResult result = relay.relay(batchSize);
        if (result.failed() > 0) {
            LOG.warn("Relevo con fallas: {}", result);
        }
        return result.published();
    }
}
