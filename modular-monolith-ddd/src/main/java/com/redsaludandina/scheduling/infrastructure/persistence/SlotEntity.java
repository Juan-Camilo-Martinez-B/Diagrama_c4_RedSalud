package com.redsaludandina.scheduling.infrastructure.persistence;

import com.redsaludandina.scheduling.domain.model.Booking;
import com.redsaludandina.scheduling.domain.model.Slot;
import com.redsaludandina.scheduling.domain.model.TimeWindow;
import com.redsaludandina.shared.domain.ClinicalId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila JPA de un cupo; la reserva vive en la misma fila (a lo sumo una por cupo). */
@Entity
@Table(schema = "scheduling", name = "slot")
public class SlotEntity {

    @Id
    private String id;

    @Column(name = "specialist_id", nullable = false)
    private String specialistId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "booking_id")
    private String bookingId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "center_id")
    private String centerId;

    @Column(name = "booked_at")
    private Instant bookedAt;

    protected SlotEntity() {
    }

    static SlotEntity fromDomain(Slot slot) {
        SlotEntity entity = new SlotEntity();
        entity.id = slot.id().value();
        entity.specialistId = slot.specialistId();
        entity.startsAt = slot.window().start();
        entity.endsAt = slot.window().end();
        slot.booking().ifPresent(entity::copyBooking);
        return entity;
    }

    private void copyBooking(Booking booking) {
        bookingId = booking.id().value();
        patientId = booking.patientId();
        centerId = booking.centerId();
        bookedAt = booking.bookedAt();
    }

    Slot toDomain() {
        Booking booking = bookingId == null ? null
                : new Booking(ClinicalId.of(bookingId), patientId, centerId, bookedAt);
        return Slot.restore(ClinicalId.of(id), specialistId, new TimeWindow(startsAt, endsAt), booking);
    }
}
