package com.redsaludandina.identity.domain.model;

import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.Require;
import java.util.Objects;

/**
 * Perfil de acceso de un usuario autenticado en Cognito: su rol y sus atributos. Un médico rural sin
 * centro o un especialista sin especialidad no son perfiles válidos.
 */
public final class User {

    public static final String INCOMPLETE_ATTRIBUTES = "INCOMPLETE_ATTRIBUTES";

    private final String id;
    private final Role role;
    private final AccessAttributes attributes;

    public User(String id, Role role, AccessAttributes attributes) {
        this.id = Require.text(id, "userId");
        this.role = Require.notNull(role, "role");
        this.attributes = Require.notNull(attributes, "attributes");
        if (!role.acceptsAttributes(attributes)) {
            throw new BusinessRuleException(INCOMPLETE_ATTRIBUTES,
                    "El rol " + role + " exige atributos que el usuario " + id + " no tiene");
        }
    }

    /** Aplica la regla de acceso de su rol. */
    public AccessDecision evaluate(ClinicalResource resource) {
        return role.evaluate(this, Require.notNull(resource, "resource"));
    }

    public String id() {
        return id;
    }

    public Role role() {
        return role;
    }

    public AccessAttributes attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
