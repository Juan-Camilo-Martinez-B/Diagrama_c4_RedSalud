package com.redsaludandina.identity.application.port;

import com.redsaludandina.identity.domain.model.User;
import java.util.Optional;

/** Perfiles de acceso de los usuarios (puerto de salida). */
public interface UserRepository {

    Optional<User> findById(String userId);
}
