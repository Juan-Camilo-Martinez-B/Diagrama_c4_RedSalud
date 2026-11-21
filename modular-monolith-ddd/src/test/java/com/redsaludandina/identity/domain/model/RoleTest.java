package com.redsaludandina.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.identity.domain.service.AccessPolicy;
import com.redsaludandina.shared.domain.BusinessRuleException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleTest {

    private static final ClinicalResource EPISODE = new ClinicalResource("episode/1", "pac-1", "centro-a", null);
    private static final AccessPolicy POLICY = new AccessPolicy();

    private static boolean allowed(User user, ClinicalResource resource) {
        return POLICY.evaluate(Optional.of(user), resource).allowed();
    }

    @Test
    void aPatientOnlySeesTheirOwnHistory() {
        assertThat(allowed(new User("pac-1", Role.PATIENT, new AccessAttributes(null, null, Set.of())), EPISODE)).isTrue();
        assertThat(allowed(new User("pac-2", Role.PATIENT, new AccessAttributes(null, null, Set.of())), EPISODE)).isFalse();
    }

    @Test
    void aRuralDoctorNeedsTheSameCenterAndARelationWithThePatient() {
        User related = new User("med-1", Role.RURAL_DOCTOR, new AccessAttributes("centro-a", null, Set.of("pac-1")));
        User otherCenter = new User("med-2", Role.RURAL_DOCTOR, new AccessAttributes("centro-b", null, Set.of("pac-1")));
        User unrelated = new User("med-3", Role.RURAL_DOCTOR, new AccessAttributes("centro-a", null, Set.of()));

        assertThat(allowed(related, EPISODE)).isTrue();
        assertThat(allowed(otherCenter, EPISODE)).isFalse();
        assertThat(allowed(unrelated, EPISODE)).isFalse();
    }

    @Test
    void aSpecialistNeedsTheRequiredSpecialty() {
        User cardiologist = new User("esp-1", Role.SPECIALIST, new AccessAttributes(null, "Cardiología", Set.of("pac-1")));
        ClinicalResource dermatology = new ClinicalResource("episode/1", "pac-1", "centro-a", "Dermatología");

        assertThat(allowed(cardiologist, EPISODE)).isTrue();
        assertThat(allowed(cardiologist, dermatology)).isFalse();
    }

    @Test
    void aUserWithoutProfileIsDeniedAndIncompleteProfilesAreInvalid() {
        assertThat(POLICY.evaluate(Optional.empty(), EPISODE).allowed()).isFalse();
        assertThatThrownBy(() -> new User("med-9", Role.RURAL_DOCTOR, new AccessAttributes(null, null, Set.of())))
                .isInstanceOf(BusinessRuleException.class);
    }
}
