package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.DomainEvent;
import java.util.List;

/**
 * Lenguaje Publicado (PL): los únicos eventos que cruzan de un contexto a otro. Es la única vía de
 * comunicación asíncrona entre contextos.
 */
public final class PublishedLanguage {

    public static final List<Class<? extends DomainEvent>> EVENTS = List.of(
            SlotBooked.class,
            PatientAttended.class,
            PrescriptionSigned.class,
            ClinicalRead.class,
            ClinicalValueDiscarded.class,
            ReferralRequested.class,
            ReferralClosed.class,
            AccessDenied.class);

    private PublishedLanguage() {
    }
}
