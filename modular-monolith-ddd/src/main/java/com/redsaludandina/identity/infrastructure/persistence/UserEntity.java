package com.redsaludandina.identity.infrastructure.persistence;

import com.redsaludandina.identity.domain.model.AccessAttributes;
import com.redsaludandina.identity.domain.model.Role;
import com.redsaludandina.identity.domain.model.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** Fila JPA del perfil de acceso. El dominio no la conoce: se traduce con {@link #toDomain()}. */
@Entity
@Table(schema = "identity", name = "user_account")
public class UserEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String role;

    @Column(name = "center_id")
    private String centerId;

    private String specialty;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "identity", name = "related_patient", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "patient_id")
    private Set<String> relatedPatients = new HashSet<>();

    protected UserEntity() {
    }

    User toDomain() {
        return new User(id, Role.valueOf(role), new AccessAttributes(centerId, specialty, relatedPatients));
    }
}
