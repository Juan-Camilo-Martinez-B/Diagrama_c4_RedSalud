package com.redsaludandina.scheduling.domain.model;

import com.redsaludandina.shared.domain.AggregateRoot;
import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.SlotBooked;
import java.time.Instant;
import java.util.Optional;

/**
 * Cupo de un especialista (raíz de agregado de Agenda). Reglas del caso: sin doble reserva y la
 * reserva se resuelve SOLO en el servidor (nunca offline, Caso §4).
 */
public final class Slot extends AggregateRoot<ClinicalId> {

    public static final String SLOT_ALREADY_BOOKED = "SLOT_ALREADY_BOOKED";
    public static final String SLOT_ALREADY_STARTED = "SLOT_ALREADY_STARTED";

    private final ClinicalId id;
    private final String specialistId;
    private final TimeWindow window;
    private Booking booking;

    private Slot(ClinicalId id, String specialistId, TimeWindow window, Booking booking) {
        this.id = Require.notNull(id, "slotId");
        this.specialistId = Require.text(specialistId, "specialistId");
        this.window = Require.notNull(window, "window");
        this.booking = booking;
    }

    public static Slot publish(ClinicalId id, String specialistId, TimeWindow window) {
        return new Slot(id, specialistId, window, null);
    }

    public static Slot restore(ClinicalId id, String specialistId, TimeWindow window, Booking booking) {
        return new Slot(id, specialistId, window, booking);
    }

    /** Reservar el mismo cupo con la MISMA reserva es idempotente; con otra, se rechaza. */
    public void book(ClinicalId bookingId, String patientId, String centerId, Instant now) {
        Require.notNull(bookingId, "bookingId");
        if (booking != null) {
            rejectUnlessSameBooking(bookingId);
            return;
        }
        if (window.hasStarted(Require.notNull(now, "now"))) {
            throw new BusinessRuleException(SLOT_ALREADY_STARTED, "El cupo " + id + " ya empezó y no se puede reservar");
        }
        booking = new Booking(bookingId, patientId, centerId, now);
        record(new SlotBooked(id, bookingId, booking.patientId(), booking.centerId(), specialistId, window.start(), now));
    }

    private void rejectUnlessSameBooking(ClinicalId bookingId) {
        if (!booking.id().equals(bookingId)) {
            throw new BusinessRuleException(SLOT_ALREADY_BOOKED,
                    "El cupo " + id + " ya está reservado; no se admite doble reserva");
        }
    }

    public SlotStatus status() {
        return booking == null ? SlotStatus.AVAILABLE : SlotStatus.BOOKED;
    }

    @Override
    public ClinicalId id() {
        return id;
    }

    public String specialistId() {
        return specialistId;
    }

    public TimeWindow window() {
        return window;
    }

    public Optional<Booking> booking() {
        return Optional.ofNullable(booking);
    }
}
