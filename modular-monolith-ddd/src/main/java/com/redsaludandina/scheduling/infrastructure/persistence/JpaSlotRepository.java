package com.redsaludandina.scheduling.infrastructure.persistence;

import com.redsaludandina.scheduling.application.port.SlotRepository;
import com.redsaludandina.scheduling.domain.model.Slot;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Adaptador del lado de escritura de Agenda sobre Spring Data JPA. */
public final class JpaSlotRepository implements SlotRepository {

    private final SlotJpaRepository jpa;

    public JpaSlotRepository(SlotJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(ClinicalId slotId) {
        return jpa.existsById(slotId.value());
    }

    @Override
    public Optional<Slot> findForUpdate(ClinicalId slotId) {
        return jpa.findWithLockById(slotId.value()).map(SlotEntity::toDomain);
    }

    @Override
    public void save(Slot slot) {
        jpa.save(SlotEntity.fromDomain(slot));
    }
}
