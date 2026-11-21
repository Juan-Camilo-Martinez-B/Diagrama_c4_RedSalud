package com.redsaludandina.legacy.infrastructure.acl.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila {@code legacy.job_state}: hasta cuándo se materializó el historial en la última corrida. */
@Entity
@Table(schema = "legacy", name = "job_state")
public class JobStateEntity {

    @Id
    private String name;

    @Column(name = "last_run", nullable = false)
    private Instant lastRun;

    protected JobStateEntity() {
    }

    JobStateEntity(String name, Instant lastRun) {
        this.name = name;
        this.lastRun = lastRun;
    }

    Instant lastRun() {
        return lastRun;
    }
}
