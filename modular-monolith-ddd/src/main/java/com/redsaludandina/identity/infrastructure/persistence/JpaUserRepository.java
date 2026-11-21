package com.redsaludandina.identity.infrastructure.persistence;

import com.redsaludandina.identity.application.port.UserRepository;
import com.redsaludandina.identity.domain.model.User;
import java.util.Optional;

/** Adaptador del puerto {@link UserRepository} sobre Spring Data JPA. */
public final class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(String userId) {
        return jpa.findById(userId).map(UserEntity::toDomain);
    }
}
