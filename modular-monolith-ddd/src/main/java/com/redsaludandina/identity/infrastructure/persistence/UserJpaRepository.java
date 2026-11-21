package com.redsaludandina.identity.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data de los perfiles (solo lectura: los perfiles se administran fuera del monolito). */
public interface UserJpaRepository extends Repository<UserEntity, String> {

    Optional<UserEntity> findById(String id);
}
