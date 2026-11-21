package com.redsaludandina.identity.domain.model;

/**
 * Rol del usuario. Cada rol sabe qué atributos exige y cómo decide el acceso a un recurso clínico
 * (Caso §5: autorización por rol Y atributo).
 *
 * <ul>
 *   <li>Paciente: solo su propia historia.</li>
 *   <li>Médico rural: recursos de SU centro y de pacientes con los que tiene relación de atención.</li>
 *   <li>Especialista: recursos de su especialidad (si el recurso exige una) y de pacientes relacionados.</li>
 * </ul>
 */
public enum Role {

    PATIENT {
        @Override
        boolean acceptsAttributes(AccessAttributes attributes) {
            return true;
        }

        @Override
        AccessDecision evaluate(User user, ClinicalResource resource) {
            return user.id().equals(resource.patientId())
                    ? AccessDecision.allow("Es su propia historia clínica")
                    : AccessDecision.deny("Un paciente solo accede a su propia historia clínica");
        }
    },

    RURAL_DOCTOR {
        @Override
        boolean acceptsAttributes(AccessAttributes attributes) {
            return attributes.center().isPresent();
        }

        @Override
        AccessDecision evaluate(User user, ClinicalResource resource) {
            return user.attributes().belongsTo(resource.centerId())
                    ? requireRelation(user, resource)
                    : AccessDecision.deny("El recurso es de otro centro");
        }
    },

    SPECIALIST {
        @Override
        boolean acceptsAttributes(AccessAttributes attributes) {
            return attributes.declaredSpecialty().isPresent();
        }

        @Override
        AccessDecision evaluate(User user, ClinicalResource resource) {
            return user.attributes().coversSpecialty(resource.requiredSpecialty())
                    ? requireRelation(user, resource)
                    : AccessDecision.deny("El recurso es de otra especialidad");
        }
    };

    abstract boolean acceptsAttributes(AccessAttributes attributes);

    abstract AccessDecision evaluate(User user, ClinicalResource resource);

    private static AccessDecision requireRelation(User user, ClinicalResource resource) {
        return user.attributes().isRelatedTo(resource.patientId())
                ? AccessDecision.allow("Tiene relación de atención con el paciente")
                : AccessDecision.deny("No tiene relación de atención con el paciente");
    }
}
