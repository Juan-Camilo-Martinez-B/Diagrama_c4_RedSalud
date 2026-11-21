package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.application.port.AccessLogWriter;
import com.redsaludandina.insights.domain.audit.AccessRecord;

/** Evidencia de accesos local (perfil local y pruebas) sobre Spring Data JPA. */
public final class JpaAccessLogWriter implements AccessLogWriter {

    private final AccessRecordJpaRepository jpa;

    public JpaAccessLogWriter(AccessRecordJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean appendIfAbsent(AccessRecord accessRecord) {
        if (jpa.existsById(accessRecord.id())) {
            return false;
        }
        jpa.save(new AccessRecordEntity(accessRecord));
        return true;
    }
}
