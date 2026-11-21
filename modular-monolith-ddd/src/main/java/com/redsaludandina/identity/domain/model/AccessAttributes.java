package com.redsaludandina.identity.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.util.Optional;
import java.util.Set;

/**
 * Atributos con los que se decide el acceso además del rol (Caso §5): centro, especialidad y los
 * pacientes con los que el usuario tiene relación de atención.
 */
public record AccessAttributes(String centerId, String specialty, Set<String> relatedPatients) {

    public AccessAttributes {
        centerId = Require.optionalText(centerId);
        specialty = Require.optionalText(specialty);
        relatedPatients = Set.copyOf(Require.notNull(relatedPatients, "relatedPatients"));
    }

    public Optional<String> center() {
        return Optional.ofNullable(centerId);
    }

    public Optional<String> declaredSpecialty() {
        return Optional.ofNullable(specialty);
    }

    public boolean belongsTo(String otherCenterId) {
        return otherCenterId.equals(centerId);
    }

    /** Sin especialidad exigida, cualquier especialista la cubre. */
    public boolean coversSpecialty(Optional<String> requiredSpecialty) {
        return requiredSpecialty.map(required -> required.equals(specialty)).orElse(true);
    }

    public boolean isRelatedTo(String patientId) {
        return relatedPatients.contains(patientId);
    }
}
