package com.redsaludandina.identity.domain.service;

import com.redsaludandina.identity.domain.model.AccessDecision;
import com.redsaludandina.identity.domain.model.ClinicalResource;
import com.redsaludandina.identity.domain.model.User;
import java.util.Optional;

/**
 * Servicio de dominio: decide si un usuario puede acceder a un recurso clínico. Un usuario sin perfil
 * de acceso nunca accede (negar por defecto).
 */
public final class AccessPolicy {

    public AccessDecision evaluate(Optional<User> user, ClinicalResource resource) {
        return user.map(profile -> profile.evaluate(resource))
                .orElseGet(() -> AccessDecision.deny("Usuario sin perfil de acceso"));
    }
}
