package com.redsaludandina.scheduling.infrastructure.persistence;

import com.redsaludandina.scheduling.application.query.AvailableSlotView;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data del lado de LECTURA: solo consultas que proyectan directamente al DTO de
 * lectura; no expone {@code save} ni devuelve entidades.
 */
public interface SlotQueryRepository extends Repository<SlotEntity, String> {

    @Query("select new com.redsaludandina.scheduling.application.query.AvailableSlotView("
            + "s.id, s.specialistId, s.startsAt, s.endsAt) from SlotEntity s"
            + " where s.specialistId = :specialistId and s.bookingId is null and s.startsAt > :from"
            + " order by s.startsAt")
    List<AvailableSlotView> findAvailable(@Param("specialistId") String specialistId, @Param("from") Instant from);
}
